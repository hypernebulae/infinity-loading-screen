package hypernebulae.loading.mixin;

import hypernebulae.loading.ReloadOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyVariable(method = "setOverlay", at = @At("HEAD"), argsOnly = true)
    private Overlay loading$replaceReloadOverlay(Overlay overlay) {
        if (overlay instanceof LoadingOverlay vanilla) {
            LoadingOverlayAccessor accessor = (LoadingOverlayAccessor) (Object) vanilla;
            return new ReloadOverlay(Minecraft.getInstance(), accessor.loading$getReload(), accessor.loading$getOnFinish());
        }
        return overlay;
    }
}