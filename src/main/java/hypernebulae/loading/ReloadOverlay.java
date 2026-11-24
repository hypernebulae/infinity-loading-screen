package hypernebulae.loading;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Consumer;

public class ReloadOverlay extends Overlay {
    private final Minecraft mc;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean dark;
    private final Draw.Tex image;
    private final int bg;
    private final boolean inWorld;
    private float currentProgress;
    private long lastTick = -1L;
    private boolean fading;
    private float fadeMs;

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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();
        long dt = lastTick < 0 ? 0 : Math.min(now - lastTick, 50L); // a load stall pauses instead of skipping
        lastTick = now;

        if (!fading && reload.isDone()) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                onFinish.accept(Optional.of(t));
            }
            fading = true;
            fadeMs = 0f;
            if (mc.screen != null) mc.screen.init(mc, w, h);
        } else if (fading) {
            fadeMs += dt;
        }

        float fade = fading ? Mth.clamp(fadeMs / 1000f, 0f, 1f) : 0f;
        float alpha = 1f - fade;

        if (fading && mc.screen != null) mc.screen.render(g, 0, 0, partialTick);
        int fillAlpha = inWorld ? 0 : Mth.ceil(alpha * 255f);
        if (fillAlpha > 0) g.fill(0, 0, w, h, fillAlpha << 24 | bg);

        int cx = w / 2, cy = h / 2;
        Draw.blitCentered(g, image, cx, cy, 256, 256, Math.round(255 * alpha));

        float target = reload.isDone() ? 1f : reload.getActualProgress();
        float k = 1f - (float) Math.pow(0.95, dt / 16.667);
        currentProgress = Mth.clamp(currentProgress + (target - currentProgress) * k, 0f, 1f);
        if (fading && currentProgress > 0.99f) currentProgress = 1f;

        int x0 = cx - 150, y0 = cy + 50;
        Draw.progressBar(g, x0, y0, x0 + 300, y0 + 10, currentProgress, alpha, dark);

        if (fade >= 1f) mc.setOverlay(null);
    }
}