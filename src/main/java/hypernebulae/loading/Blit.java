package hypernebulae.loading;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

final class Blit {
    static void draw(GuiGraphics g, Draw.Tex t, int x, int y, int w, int h, int alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha / 255f);
        g.blit(t.id(), x, y, w, h, 0f, 0f, t.w(), t.h(), t.w(), t.h());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}