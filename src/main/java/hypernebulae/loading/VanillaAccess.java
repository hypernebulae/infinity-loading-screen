package hypernebulae.loading;

import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public record VanillaAccess(ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
    @SuppressWarnings("unchecked")
    public static VanillaAccess of(LoadingOverlay o) throws Exception {
        ReloadInstance reload = null;
        Consumer<Optional<Throwable>> onFinish = null;
        boolean fadeIn = false;
        for (Field f : LoadingOverlay.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            Class<?> t = f.getType();
            if (ReloadInstance.class.isAssignableFrom(t)) reload = (ReloadInstance) f.get(o);
            else if (t == Consumer.class) onFinish = (Consumer<Optional<Throwable>>) f.get(o);
            else if (t == boolean.class) fadeIn = f.getBoolean(o);
        }
        if (reload == null || onFinish == null) throw new IllegalStateException("LoadingOverlay fields not found");
        return new VanillaAccess(reload, onFinish, fadeIn);
    }

    public static boolean isDark() {
        try {
            for (Field f : LoadingOverlay.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && IntSupplier.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (((IntSupplier) f.get(null)).getAsInt() & 0xFFFFFF) == 0;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}