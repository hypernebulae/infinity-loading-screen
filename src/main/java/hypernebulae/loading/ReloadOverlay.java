package hypernebulae.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.function.Consumer;

public class ReloadOverlay extends BaseOverlay {
    private final Minecraft mc;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean dark, inWorld;
    private final Identifier image;
    private final int bg;
    private float currentProgress, fadeMs;
    private long lastTick = -1L;
    private boolean fading;

    public ReloadOverlay(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish) {
        this.mc = mc;
        this.reload = reload;
        this.onFinish = onFinish;
        this.dark = mc.options.darkMojangStudiosBackground().get();
        this.image = Identifier.fromNamespaceAndPath("loading",
                dark ? "textures/gui/mojang_dark.png" : "textures/gui/mojang.png");
        this.bg = dark ? 0x000000 : 0xFFFFFF;
        this.inWorld = mc.level != null;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, float pt) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();
        long dt = lastTick < 0 ? 0 : Math.min(now - lastTick, 50L);
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
            Screen s = Compat.screen(mc);
            if (s != null) Compat.initScreen(mc, s, w, h);
        } else if (fading) {
            fadeMs += dt;
        }

        float fade = fading ? Mth.clamp(fadeMs / 1000f, 0f, 1f) : 0f;
        float alpha = 1f - fade;

        Screen s = Compat.screen(mc);
        if (fading && s != null) Compat.renderScreen(s, g, pt);
        int fillAlpha = inWorld ? 0 : Mth.ceil(alpha * 255f);
        if (fillAlpha > 0) {
            g.nextStratum();
            g.fill(0, 0, w, h, fillAlpha << 24 | bg);
        }
        g.nextStratum();

        int cx = w / 2, cy = h / 2;
        int a255 = Math.round(255 * alpha);
        if (a255 > 0) Compat.blit(g, image, cx - 128, cy - 128, 256, 256, ARGB.color(a255, 255, 255, 255));

        float target = reload.isDone() ? 1f : reload.getActualProgress();
        float k = 1f - (float) Math.pow(0.95, dt / 16.667);
        currentProgress = Mth.clamp(currentProgress + (target - currentProgress) * k, 0f, 1f);
        if (fading && currentProgress > 0.99f) currentProgress = 1f;

        int x0 = cx - 150, y0 = cy + 50;
        bar(g, x0, y0, x0 + 300, y0 + 10, alpha);

        if (fade >= 1f) Compat.setOverlay(mc, null);
    }

    private void bar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24;
        int m = Mth.ceil((x1 - x0 - 1) * currentProgress);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | (dark ? 0xFFFFFF : 0x000000));
        g.fill(x0, y0, x1, y1, a | (dark ? 0x000000 : 0xFFFFFF));
        g.fill(x0 + 1, y0 + 1, x0 + m, y1 - 1, a | 0xE22837);
    }
}