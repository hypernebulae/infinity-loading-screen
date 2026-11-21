package hypernebulae.loading;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.GuiPipeline;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureHandle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

import java.util.Optional;
import java.util.function.Consumer;

public class LogoOverlay extends Overlay {
    private LogoOverlay.Stage stage = LogoOverlay.Stage.INIT;
    private long textFadeInStart = -1L;
    private float logoFlyInProgress;
    private long lastTick;
    private SoundInstance logoSound;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private final LogoOverlay.Effect effect;
    private final boolean dark;
    private final Identifier LOGO_TEXTURE;
    private final Identifier TEXT_TEXTURE;
    private final int BACKGROUND;

    public LogoOverlay(Minecraft minecraft, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> consumer) {
        this.minecraft = minecraft;
        this.reload = reloadInstance;
        this.onFinish = consumer;
        this.dark = minecraft.options.darkMojangStudiosBackground().get();
        this.LOGO_TEXTURE = Identifier.fromNamespaceAndPath("loading", this.dark ? "textures/gui/mojang_logo_dark.png" : "textures/gui/mojang_logo.png");
        this.TEXT_TEXTURE = Identifier.fromNamespaceAndPath("loading", this.dark ? "textures/gui/mojang_text_dark.png" : "textures/gui/mojang_text.png");
        this.BACKGROUND = this.dark ? 0xFF000000 : 0xFFFFFFFF;
        LogoOverlay.Effect[] effects = LogoOverlay.Effect.values();
        this.effect = effects[(int) (System.currentTimeMillis() % effects.length)];
    }

    private float fadeOutAnim() {
        return this.fadeOutStart > -1L ? (float)(Util.getMillis() - this.fadeOutStart) / 1000.0F : -1.0F;
    }

    private static int replaceAlpha(final int color, final int alpha) {
        return color & 16777215 | alpha << 24;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics) {
        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        float fadeOutAnim = this.fadeOutAnim();
        float logoAlpha;
        if (fadeOutAnim >= 0.0F) {
            Screen screen = this.minecraft.gui.screen();
            if (screen != null) {
                graphics.extractWithoutMouse(() -> screen.extractRenderStateWithTooltipAndSubtitles(graphics));
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }

            float t = Math.clamp(fadeOutAnim, 0.0F, 1.0F);
            int alpha = Mth.ceil((1.0F - t) * 255.0F);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, replaceAlpha(BACKGROUND, alpha));
            logoAlpha = 1.0F - t;

            if (fadeOutAnim >= 1.0F) {
                this.minecraft.gui.setOverlay(null);
                return;
            }
        } else {
            ARGB.setVector4fFromARGB32(this.minecraft.gameRenderer.gameRenderState().guiRenderState.clearColorOverride, BACKGROUND);
            logoAlpha = 1.0F;
        }
        long m = Util.getMillis();
        long n = m - this.lastTick;
        this.lastTick = m;
        if (this.stage == LogoOverlay.Stage.INIT) {
            this.stage = LogoOverlay.Stage.FLY;
            this.logoFlyInProgress = 10.0F;
            this.textFadeInStart = -1L;
        } else {
            if (this.stage == LogoOverlay.Stage.FLY) {
                this.logoFlyInProgress -= (float)n / 500.0F;
                if (this.logoFlyInProgress <= 0.0F) {
                    this.stage = LogoOverlay.Stage.WAIT_FOR_LOAD;
                }
            } else if (this.stage == LogoOverlay.Stage.WAIT_FOR_LOAD) {
                if (this.reload.isDone()) {
                    this.textFadeInStart = m;
                    this.logoSound = SimpleSoundInstance.forUI(
                            CoolSoundEvents.AWESOME_INTRO, 1.0F, 0.25F
                    );
                    this.minecraft.getSoundManager().play(this.logoSound);
                    this.stage = LogoOverlay.Stage.TEXT;
                }
            } else if (this.fadeOutStart == -1L
                    && this.textFadeInStart != -1L
                    && m - this.textFadeInStart > 1000L
                    && !this.minecraft.getSoundManager().isActive(this.logoSound)) {

                try {
                    this.reload.checkExceptions();
                    this.onFinish.accept(Optional.empty());
                } catch (Throwable throwable) {
                    this.onFinish.accept(Optional.of(throwable));
                }

                if (this.minecraft.gui.screen() != null) {
                    Window window = this.minecraft.getWindow();
                    this.minecraft.gui.screen().init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
                }

                this.fadeOutStart = Util.getMillis();
            }

            int o = Math.clamp((int)(m - this.textFadeInStart), 0, 255);
            if (this.textFadeInStart != -1L) {
                this.blit(graphics, TEXT_TEXTURE, width / 2, height - height / 8, 208, 38, Math.round(o * logoAlpha));
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(width / 2.0F, height / 2.0F);
            switch (this.effect) {
                case CLASSIC: {
                    float g = 20.0F * this.logoFlyInProgress;
                    float h = 100.0F * Mth.sin(this.logoFlyInProgress);
                    graphics.pose().rotate((float) Math.toRadians(g));
                    graphics.pose().translate(h, 0.0F);
                    float p = 1.0F / (2.0F * this.logoFlyInProgress + 1.0F);
                    graphics.pose().rotate((float) Math.toRadians(1.5F * this.logoFlyInProgress));
                    graphics.pose().scale(p, p);
                    break;
                }
                case SPRING: {
                    float g = 40.0F * ((float)Math.exp(this.logoFlyInProgress / 3.0F) - 1.0F) * Mth.sin(this.logoFlyInProgress);
                    graphics.pose().translate(g, 0.0F);
                    break;
                }
                case SLOWDOWN: {
                    float g = (float) Math.exp(this.logoFlyInProgress) - 1.0F;
                    graphics.pose().rotate((float) Math.toRadians(g));
                    break;
                }
                case REVERSE: {
                    float g = Mth.cos(this.logoFlyInProgress / 10.0F * (float) Math.PI);
                    graphics.pose().scale(g, g);
                    break;
                }
                case GROW: {
                    float g = (1.0F - this.logoFlyInProgress / 10.0F) * 0.75F;
                    float h = 2.0F * Mth.sin(g * (float) Math.PI);
                    graphics.pose().scale(h, h);
                }
            }

            if (this.fadeOutStart == -1L) {
                float g = this.reload.getActualProgress();
                this.currentProgress = Math.clamp(this.currentProgress * 0.99F + g * 0.00999999F, 0.0F, 1.0F);
            }
            this.extractProgressBar(graphics, -39, 38, 39, 48, this.currentProgress != 1.0F ? 1.0F : 0.0F, logoAlpha);
            this.blit(graphics, LOGO_TEXTURE, 0, 0, 78, 76, Math.round(255 * logoAlpha));
            graphics.pose().popMatrix();
        }
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier textureId, int x, int y, int w, int h, int alpha) {
        int halfW = w / 2;
        int halfH = h / 2;
        TextureHandle texture = this.minecraft.getTextureManager().getTexture(textureId);
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

    private void extractProgressBar(final GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float fade, float alpha) {
        int a = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
        int m = Mth.ceil((x1 - x0 - 1) * this.currentProgress);
        int border = Math.round((1.0F - fade) * 255.0F);
        if (dark) border = 255 - border;
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | border << 16 | border << 8 | border);
        graphics.fill(x0, y0, x1, y1, a | (dark ? 0x000000 : 0xFFFFFF));
        graphics.fill(
                x0 + 1,
                y0 + 1,
                x0 + m,
                y1 - 1,
                a | (int) Mth.lerp(1.0F - fade, 226.0F, 255.0F) << 16 | (int) Mth.lerp(1.0F - fade, 40.0F, 255.0F) << 8 | (int) Mth.lerp(1.0F - fade, 55.0F, 255.0F)
        );
    }

    enum Effect {
        CLASSIC,
        SPRING,
        SLOWDOWN,
        REVERSE,
        GROW
    }

    enum Stage {
        INIT,
        FLY,
        WAIT_FOR_LOAD,
        TEXT
    }
}