package hypernebulae.loading;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Consumer;

public class LogoOverlay extends Overlay {
    private enum Effect { CLASSIC, SPRING, SLOWDOWN, REVERSE, GROW }
    private enum Stage { INIT, FLY, WAIT_FOR_LOAD, TEXT }
    private Stage stage = Stage.INIT;
    private long textFadeInStart = -1L, lastTick, fadeOutStart = -1L;
    private float flyIn, currentProgress;
    private SoundInstance logoSound;
    private final Minecraft mc;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final Effect effect;
    private final boolean dark;
    private final ResourceLocation logoTex, textTex;
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
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float fadeOutAnim = fadeOutStart > -1L ? (Util.getMillis() - fadeOutStart) / 1000f : -1f;
        float logoAlpha;

        if (fadeOutAnim >= 0f) {
            if (mc.screen != null) mc.screen.render(pose, 0, 0, partialTick);
            float t = Mth.clamp(fadeOutAnim, 0f, 1f);
            Draw.fill(0, 0, w, h, Mth.ceil((1f - t) * 255f) << 24 | bg);
            logoAlpha = 1f - t;
            if (fadeOutAnim >= 1f) { mc.setOverlay(null); return; }
        } else {
            Draw.fill(0, 0, w, h, 0xFF000000 | bg);
            logoAlpha = 1f;
        }

        long now = Util.getMillis();
        long dt = now - lastTick;
        lastTick = now;

        if (stage == Stage.INIT) {
            stage = Stage.FLY;
            flyIn = 10f;
            return;
        }

        if (stage == Stage.FLY) {
            flyIn -= dt / 500f;
            if (flyIn <= 0f) stage = Stage.WAIT_FOR_LOAD;
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
        } else if (fadeOutStart == -1L && textFadeInStart != -1L && now - textFadeInStart > 1000L
                && (logoSound == null || !mc.getSoundManager().isActive(logoSound))) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                onFinish.accept(Optional.of(t));
            }
            if (mc.screen != null) mc.screen.init(mc, w, h);
            fadeOutStart = Util.getMillis();
        }

        if (textFadeInStart != -1L) {
            int o = Mth.clamp((int) (now - textFadeInStart), 0, 255);
            Draw.texCentered(new Affine(), textTex, w / 2f, h - h / 8f, 208, 38, Math.round(o * logoAlpha));
        }

        Affine t = new Affine();
        t.translate(w / 2f, h / 2f);
        switch (effect) {
            case CLASSIC -> {
                t.rotateDeg(20f * flyIn);
                t.translate(100f * Mth.sin(flyIn), 0f);
                float p = 1f / (2f * flyIn + 1f);
                t.rotateDeg(1.5f * flyIn);
                t.scale(p, p);
            }
            case SPRING -> t.translate(40f * ((float) Math.exp(flyIn / 3f) - 1f) * Mth.sin(flyIn), 0f);
            case SLOWDOWN -> t.rotateDeg((float) Math.exp(flyIn) - 1f);
            case REVERSE -> { float g = Mth.cos(flyIn / 10f * (float) Math.PI); t.scale(g, g); }
            case GROW -> {
                float g = (1f - flyIn / 10f) * 0.75f;
                float s = 2f * Mth.sin(g * (float) Math.PI);
                t.scale(s, s);
            }
        }

        if (fadeOutStart == -1L) {
            currentProgress = Mth.clamp(currentProgress * 0.99f + reload.getActualProgress() * 0.01f, 0f, 1f);
        }
        Draw.progressBar(t, -39, 38, 39, 48, currentProgress, logoAlpha, dark);
        Draw.texCentered(t, logoTex, 0, 0, 78, 76, Math.round(255 * logoAlpha));
    }
}