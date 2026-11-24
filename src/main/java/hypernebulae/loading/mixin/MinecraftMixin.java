package hypernebulae.loading.mixin;

import hypernebulae.loading.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @ModifyVariable(method = "setOverlay", at = @At("HEAD"), argsOnly = true)
    private Overlay loading$replaceOverlay(Overlay overlay) {
        if (overlay instanceof LoadingOverlay vanilla) {
            try {
                Minecraft mc = (Minecraft) (Object) this;
                VanillaAccess v = VanillaAccess.of(vanilla);
                return v.fadeIn()
                        ? new ReloadOverlay(mc, v.reload(), v.onFinish())
                        : new LogoOverlay(mc, v.reload(), v.onFinish());
            } catch (Throwable t) {
                System.err.println("[loading] falling back to vanilla overlay: " + t);
            }
        }
        return overlay;
    }
}