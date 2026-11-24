package hypernebulae.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;

public final class Swap {
    private static boolean first = true;

    @SuppressWarnings("unchecked")
    public static Overlay pick(Overlay overlay) {
        if (!(overlay instanceof LoadingOverlay vanilla)) return overlay;
        try {
            ReloadInstance reload = null;
            Consumer<Optional<Throwable>> onFinish = null;
            for (Field f : LoadingOverlay.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                if (ReloadInstance.class.isAssignableFrom(f.getType())) reload = (ReloadInstance) f.get(vanilla);
                else if (f.getType() == Consumer.class) onFinish = (Consumer<Optional<Throwable>>) f.get(vanilla);
            }
            if (reload == null || onFinish == null) throw new IllegalStateException("fields not found");
            Minecraft mc = Minecraft.getInstance();
            boolean initial = first;
            first = false;
            return initial ? new LogoOverlay(mc, reload, onFinish) : new ReloadOverlay(mc, reload, onFinish);
        } catch (Throwable t) {
            System.err.println("[loading] falling back to vanilla overlay: " + t);
            return overlay;
        }
    }
}