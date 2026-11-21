package hypernebulae.loading;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.GuiPipeline;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureHandle;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

import java.util.Optional;
import java.util.function.Consumer;

public class ReloadOverlay extends Overlay {
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 256;
    private static final int BAR_OFFSET_X = 0;
    private static final int BAR_OFFSET_Y = 50;
    private static final int BAR_WIDTH = 300;
    private static final int BAR_HEIGHT = 10;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean dark;
    private final Identifier imageTexture;
    private final int backgroundColor;
    private final int backgroundAlpha;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private final boolean inWorld;

    public ReloadOverlay(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish) {
        this.minecraft = minecraft;
        this.reload = reload;
        this.onFinish = onFinish;
        this.dark = minecraft.options.darkMojangStudiosBackground().get();
        this.imageTexture = Identifier.fromNamespaceAndPath("loading", this.dark ? "textures/gui/mojang_dark.png" : "textures/gui/mojang.png");
        this.backgroundColor = this.dark ? 0xFF000000 : 0xFFFFFFFF;
        this.inWorld = minecraft.level != null;
        this.backgroundAlpha = this.inWorld ? 0 : (this.backgroundColor >>> 24);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics) {
        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();

        // Reload finished: notify vanilla and start the 1 second fade
        if (this.fadeOutStart == -1L && this.reload.isDone()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                this.onFinish.accept(Optional.of(t));
            }
            this.fadeOutStart = now;

            Screen screen = this.minecraft.gui.screen();
            if (screen != null) {
                Window window = this.minecraft.getWindow();
                screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }

        float fade = this.fadeOutStart > -1L ? Math.clamp((now - this.fadeOutStart) / 1000.0F, 0.0F, 1.0F) : 0.0F;
        float alpha = 1.0F - fade;

        int fillAlpha = Mth.ceil(alpha * this.backgroundAlpha);

        if (!this.inWorld) {
            ARGB.setVector4fFromARGB32(
                    this.minecraft.gameRenderer.gameRenderState().guiRenderState.clearColorOverride,
                    replaceAlpha(this.backgroundColor, fillAlpha));
        }

        if (this.fadeOutStart > -1L) {
            Screen screen = this.minecraft.gui.screen();
            if (screen != null) {
                graphics.extractWithoutMouse(() -> screen.extractRenderStateWithTooltipAndSubtitles(graphics));
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }
            if (fillAlpha > 0) {
                graphics.nextStratum();
                graphics.fill(0, 0, width, height, replaceAlpha(this.backgroundColor, fillAlpha));
            }
        }

        int cx = width / 2;
        int cy = height / 2;

        this.blit(graphics, cx, cy, IMAGE_WIDTH, IMAGE_HEIGHT, Math.round(255 * alpha));

        float target = this.reload.isDone() ? 1.0F : this.reload.getActualProgress();
        this.currentProgress = Math.clamp(this.currentProgress * 0.95F + target * 0.05F, 0.0F, 1.0F);

        if (this.fadeOutStart != -1L && this.currentProgress > 0.99F) {
            this.currentProgress = 1.0F;
        }

        int barX0 = cx + BAR_OFFSET_X - BAR_WIDTH / 2;
        int barY0 = cy + BAR_OFFSET_Y;
        this.extractProgressBar(graphics, barX0, barY0, barX0 + BAR_WIDTH, barY0 + BAR_HEIGHT, this.currentProgress != 1.0F ? 1.0F : 0.0F, alpha);
        // Remove the overlay only after the final (fully transparent) frame has been drawn
        if (fade >= 1.0F) {
            this.minecraft.gui.setOverlay(null);
        }
    }

    private static int replaceAlpha(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int alpha) {
        int halfW = w / 2;
        int halfH = h / 2;
        TextureHandle texture = this.minecraft.getTextureManager().getTexture(this.imageTexture);
        graphics.guiRenderState.addGuiElement(new LogoRenderState(
                GuiPipeline.TEXTURED,
                TextureSetup.singleTexture(texture.textureView(), texture.sampler()),
                new Matrix3x2f(graphics.pose()),
                x - halfW, y - halfH,
                x + halfW, y + halfH,
                0.0F, 1.0F,
                0.0F, 1.0F,
                ARGB.color(alpha, 255, 255, 255),
                graphics.scissorStack.peek()
        ));
    }

    private void extractProgressBar(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float fade, float alpha) {
        int a = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
        int m = Mth.ceil((x1 - x0 - 1) * this.currentProgress);
        int border = Math.round((1.0F - fade) * 255.0F);
        if (this.dark) border = 255 - border;
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | border << 16 | border << 8 | border);
        graphics.fill(x0, y0, x1, y1, a | (this.dark ? 0x000000 : 0xFFFFFF));
        graphics.fill(
                x0 + 1,
                y0 + 1,
                x0 + m,
                y1 - 1,
                a | (int) Mth.lerp(1.0F - fade, 226.0F, 255.0F) << 16 | (int) Mth.lerp(1.0F - fade, 40.0F, 255.0F) << 8 | (int) Mth.lerp(1.0F - fade, 55.0F, 255.0F)
        );
    }
}