package hypernebulae.loading;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;

public abstract class BaseOverlay extends Overlay {
    protected abstract void draw(GuiGraphicsExtractor g, float partialTick);

    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        draw(g, partialTick);
    }

    public void extractRenderState(GuiGraphicsExtractor g) {
        draw(g, 0f);
    }
}