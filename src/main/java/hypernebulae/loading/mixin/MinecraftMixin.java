package hypernebulae.loading.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import hypernebulae.loading.LogoOverlay;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.DevUtil;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Final
    public Gui gui;

    @Shadow
    protected abstract void selfTest();

    @Shadow
    @Final
    private ResourceLoadStateTracker reloadStateTracker;

    @Shadow
    protected abstract void onResourceLoadFinished(@Nullable GameLoadCookie loadCookie);

    @Shadow
    protected abstract void rollbackResourcePacks(Throwable t, @Nullable GameLoadCookie loadCookie);

    @Shadow
    @Final
    private TextureManager textureManager;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
    private void replaceOverlay(Gui instance, Overlay overlay, @Local ReloadInstance reloadInstance, @Local GameLoadCookie loadCookie) {
        this.gui.setOverlay(new LogoOverlay((Minecraft) (Object) this, reloadInstance, maybeT -> Util.ifElse(maybeT, t -> this.rollbackResourcePacks(t, loadCookie), () -> {
            if (DevUtil.IS_RUNNING_IN_IDE) {
                this.selfTest();
            }

            this.reloadStateTracker.finishReload();
            this.onResourceLoadFinished(loadCookie);
        })));
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;registerTextures(Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private void registerTextures(CallbackInfo ci) {
//        LogoOverlay.registerTextures(this.textureManager);
    }
}