package hypernebulae.loading;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.Util;

import java.util.Optional;
import java.util.function.Consumer;

public class ReloadOverlay extends Overlay {
    private final Minecraft mc;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean dark;
    private final ResourceLocation image;
    private final int bg;
    private final boolean inWorld;
    private float currentProgress;
    private long fadeOutStart = -1L;

    public ReloadOverlay(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish) {
        this.mc = mc;
        this.reload = reload;
        this.onFinish = onFinish;
        this.dark = VanillaAccess.isDark();
        this.image = Draw.texture(mc, dark ? "textures/gui/mojang_dark.png" : "textures/gui/mojang.png");
        this.bg = dark ? 0x000000 : 0xFFFFFF;
        this.inWorld = mc.level != null;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();

        if (fadeOutStart == -1L && reload.isDone()) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                onFinish.accept(Optional.of(t));
            }
            fadeOutStart = now;
            if (mc.screen != null) mc.screen.init(mc, w, h);
        }

        float fade = fadeOutStart > -1L ? Mth.clamp((now - fadeOutStart) / 1000f, 0f, 1f) : 0f;
        float alpha = 1f - fade;

        if (fadeOutStart > -1L && mc.screen != null) mc.screen.render(pose, 0, 0, partialTick);
        int fillAlpha = inWorld ? 0 : Mth.ceil(alpha * 255f);
        Draw.fill(0, 0, w, h, fillAlpha << 24 | bg);

        int cx = w / 2, cy = h / 2;
        Draw.texCentered(new Affine(), image, cx, cy, 256, 256, Math.round(255 * alpha));

        float target = reload.isDone() ? 1f : reload.getActualProgress();
        currentProgress = Mth.clamp(currentProgress * 0.95f + target * 0.05f, 0f, 1f);
        if (fadeOutStart != -1L && currentProgress > 0.99f) currentProgress = 1f;

        int x0 = cx - 150, y0 = cy + 50;
        Draw.progressBar(new Affine(), x0, y0, x0 + 300, y0 + 10, currentProgress, alpha, dark);

        if (fade >= 1f) mc.setOverlay(null);
    }
}