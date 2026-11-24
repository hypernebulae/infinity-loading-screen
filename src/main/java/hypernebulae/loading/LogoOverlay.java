package hypernebulae.loading;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Consumer;

public class LogoOverlay extends Overlay {
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
    private final Draw.Tex logoTex, textTex;
    private final int bg;

    public LogoOverlay(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish) {
        this.mc = mc;
        this.reload = reload;
        this.onFinish = onFinish;
        this.dark = VanillaAccess.isDark();
        this.logoTex = Draw.texture(mc, dark ? "textures/gui/mojang_logo_dark.png" : "textures/gui/mojang_logo.png");
        this.textTex = Draw.texture(mc, dark ? "textures/gui/mojang_text_dark.png" : "textures/gui/mojang_text.png");
        this.bg = dark ? 0x000000 : 0xFFFFFF;
        Effect[] e = Effect.values();
        this.effect = e[(int) (System.currentTimeMillis() % e.length)];
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = Util.getMillis();
        long dt = lastTick < 0 ? 0 : Math.min(now - lastTick, 50L); // a load stall pauses instead of skipping
        lastTick = now;

        float logoAlpha = 1f;
        if (fading) {
            fadeMs += dt;
            if (mc.screen != null) mc.screen.render(g, 0, 0, partialTick);
            float t = Mth.clamp(fadeMs / 1000f, 0f, 1f);
            g.fill(0, 0, w, h, Mth.ceil((1f - t) * 255f) << 24 | bg);
            logoAlpha = 1f - t;
            if (t >= 1f) { mc.setOverlay(null); return; }
        } else {
            g.fill(0, 0, w, h, 0xFF000000 | bg);
        }

        if (stage == Stage.INIT) {
            stage = Stage.FLY;
            flyIn = 10f;
        } else if (stage == Stage.FLY) {
            flyIn -= dt / 500f;
            if (flyIn <= 0f) { flyIn = 0f; stage = Stage.WAIT_FOR_LOAD; }
        } else if (stage == Stage.WAIT_FOR_LOAD) {
            if (reload.isDone()) {
                textFadeInStart = now;
                try {
                    logoSound = SimpleSoundInstance.forUI(CoolSoundEvents.awesomeIntro(), 1.0F, 0.25F);
                    mc.getSoundManager().play(logoSound);
                } catch (Throwable t) {
                    System.err.println("[loading] intro sound failed: " + t);
                    logoSound = null;
                }
                stage = Stage.TEXT;
            }
        } else if (!fading && textFadeInStart != -1L && now - textFadeInStart > 1000L
                && (logoSound == null || !mc.getSoundManager().isActive(logoSound))) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                onFinish.accept(Optional.of(t));
            }
            if (mc.screen != null) mc.screen.init(mc, w, h);
            fading = true;
            fadeMs = 0f;
        }

        if (textFadeInStart != -1L) {
            int o = Mth.clamp((int) (now - textFadeInStart), 0, 255);
            Draw.blitCentered(g, textTex, w / 2, h - h / 8, 208, 38, Math.round(o * logoAlpha));
        }

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(w / 2f, h / 2f, 0f);
        switch (effect) {
            case CLASSIC -> {
                ps.mulPose(Axis.ZP.rotationDegrees(20f * flyIn));
                ps.translate(100f * Mth.sin(flyIn), 0f, 0f);
                float p = 1f / (2f * flyIn + 1f);
                ps.mulPose(Axis.ZP.rotationDegrees(1.5f * flyIn));
                ps.scale(p, p, 1f);
            }
            case SPRING -> ps.translate(40f * ((float) Math.exp(flyIn / 3f) - 1f) * Mth.sin(flyIn), 0f, 0f);
            case SLOWDOWN -> ps.mulPose(Axis.ZP.rotationDegrees((float) Math.exp(flyIn) - 1f));
            case REVERSE -> { float s = Mth.cos(flyIn / 10f * (float) Math.PI); ps.scale(s, s, 1f); }
            case GROW -> {
                float s = 2f * Mth.sin((1f - flyIn / 10f) * 0.75f * (float) Math.PI);
                ps.scale(s, s, 1f);
            }
        }

        if (!fading) {
            float k = 1f - (float) Math.pow(0.99, dt / 16.667);
            currentProgress = Mth.clamp(currentProgress + (reload.getActualProgress() - currentProgress) * k, 0f, 1f);
        }
        Draw.progressBar(g, -39, 38, 39, 48, currentProgress, logoAlpha, dark);
        Draw.blitCentered(g, logoTex, 0, 0, 78, 76, Math.round(255 * logoAlpha));
        ps.popPose();
    }
}