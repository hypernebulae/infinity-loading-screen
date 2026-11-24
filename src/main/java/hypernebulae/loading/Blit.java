package hypernebulae.loading;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

final class Blit {
    static void draw(GuiGraphics g, Draw.Tex t, int x, int y, int w, int h, int alpha) {
        g.blit(RenderType::guiTextured, t.id(), x, y, 0f, 0f, w, h, t.w(), t.h(), t.w(), t.h(), alpha << 24 | 0xFFFFFF);
    }
}