package hypernebulae.loading.mixin;

import hypernebulae.loading.Swap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @ModifyVariable(method = "setOverlay", at = @At("HEAD"), argsOnly = true, require = 0)
    private Overlay loading$swap(Overlay overlay) {
        return Swap.pick(overlay);
    }
}