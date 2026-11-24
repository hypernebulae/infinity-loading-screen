package hypernebulae.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.function.Consumer;

public class LogoOverlay extends BaseOverlay {
    private enum Effect { CLASSIC, SPRING, SLOWDOWN, REVERSE, GROW }
    private enum Stage { INIT, FLY, WAIT_FOR_LOAD, TEXT }

    private Stage stage = Stage.INIT;
    private long textFadeInStart = -1L, lastTick = -1L;
    private boolean fading;
    private float fadeMs, flyIn, currentProgress;
    private SoundInstance logoSound;
    private final Minecraft mc;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final Effect effect;
    private final boolean dark;
    private final Identifier logoTex, textTex;
    private final int bg;

    public LogoOverlay(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish) {
        this.mc = mc;
        this.reload = reload;
        this.onFinish = onFinish;
        this.dark = mc.options.darkMojangStudiosBackground().get();
        this.logoTex = Identifier.fromNamespaceAndPath("loading",
                dark ? "textures/gui/mojang_logo_dark.png" : "textures/gui/mojang_logo.png");
        this.textTex = Identifier.fromNamespaceAndPath("loading",
                dark ? "textures/gui/mojang_text_dark.png" : "textures/gui/mojang_text.png");
        this.bg = dark ? 0x000000 : 0xFFFFFF;
        Effect[] e = Effect.values();
        this.effect = e[(int) (System.currentTimeMillis() % e.length)];
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, float pt) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();
        long dt = lastTick < 0 ? 0 : Math.min(now - lastTick, 50L);
        lastTick = now;

        float logoAlpha = 1f;
        if (fading) {
            fadeMs += dt;
            Screen s = Compat.screen(mc);
            if (s != null) Compat.renderScreen(s, g, pt);
            float t = Mth.clamp(fadeMs / 1000f, 0f, 1f);
            g.nextStratum();
            g.fill(0, 0, w, h, Mth.ceil((1f - t) * 255f) << 24 | bg);
            logoAlpha = 1f - t;
            if (t >= 1f) { Compat.setOverlay(mc, null); return; }
        } else {
            g.fill(0, 0, w, h, 0xFF000000 | bg);
        }
        g.nextStratum();

        if (stage == Stage.INIT) {
            stage = Stage.FLY;
            flyIn = 10f;
        } else if (stage == Stage.FLY) {
            flyIn -= dt / 500f;
            if (flyIn <= 0f) { flyIn = 0f; stage = Stage.WAIT_FOR_LOAD; }
        } else if (stage == Stage.WAIT_FOR_LOAD) {
            if (reload.isDone()) {
                textFadeInStart = now;
                logoSound = SimpleSoundInstance.forUI(CoolSoundEvents.AWESOME_INTRO, 1.0F, 0.25F);
                mc.getSoundManager().play(logoSound);
                stage = Stage.TEXT;
            }
        } else if (!fading && textFadeInStart != -1L && now - textFadeInStart > 1000L
                && !mc.getSoundManager().isActive(logoSound)) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                onFinish.accept(Optional.of(t));
            }
            Screen s = Compat.screen(mc);
            if (s != null) Compat.initScreen(mc, s, w, h);
            fading = true;
            fadeMs = 0f;
        }

        if (textFadeInStart != -1L) {
            int o = Math.round(Mth.clamp((int) (now - textFadeInStart), 0, 255) * logoAlpha);
            if (o > 0) Compat.blit(g, textTex, w / 2 - 104, h - h / 8 - 19, 208, 38, ARGB.color(o, 255, 255, 255));
        }

        g.pose().pushMatrix();
        g.pose().translate(w / 2f, h / 2f);
        switch (effect) {
            case CLASSIC -> {
                g.pose().rotate((float) Math.toRadians(20f * flyIn));
                g.pose().translate(100f * Mth.sin(flyIn), 0f);
                float p = 1f / (2f * flyIn + 1f);
                g.pose().rotate((float) Math.toRadians(1.5f * flyIn));
                g.pose().scale(p, p);
            }
            case SPRING -> g.pose().translate(40f * ((float) Math.exp(flyIn / 3f) - 1f) * Mth.sin(flyIn), 0f);
            case SLOWDOWN -> g.pose().rotate((float) Math.toRadians((float) Math.exp(flyIn) - 1f));
            case REVERSE -> { float s = Mth.cos(flyIn / 10f * (float) Math.PI); g.pose().scale(s, s); }
            case GROW -> {
                float s = 2f * Mth.sin((1f - flyIn / 10f) * 0.75f * (float) Math.PI);
                g.pose().scale(s, s);
            }
        }

        if (!fading) {
            float k = 1f - (float) Math.pow(0.99, dt / 16.667);
            currentProgress = Mth.clamp(currentProgress + (reload.getActualProgress() - currentProgress) * k, 0f, 1f);
        }
        bar(g, -39, 38, 39, 48, logoAlpha);
        int la = Math.round(255 * logoAlpha);
        if (la > 0) Compat.blit(g, logoTex, -39, -38, 78, 76, ARGB.color(la, 255, 255, 255));
        g.pose().popMatrix();
    }

    private void bar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24;
        int m = Mth.ceil((x1 - x0 - 1) * currentProgress);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | (dark ? 0xFFFFFF : 0x000000));
        g.fill(x0, y0, x1, y1, a | (dark ? 0x000000 : 0xFFFFFF));
        g.fill(x0 + 1, y0 + 1, x0 + m, y1 - 1, a | 0xE22837);
    }
}