package hypernebulae.loading;

import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class Res {
    static ResourceLocation make(String ns, String path) {
        try {
            Constructor<ResourceLocation> c = ResourceLocation.class.getDeclaredConstructor(String.class, String.class);
            c.setAccessible(true);
            return c.newInstance(ns, path);
        } catch (Throwable ignored) {}
        for (Method m : ResourceLocation.class.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == ResourceLocation.class
                    && p.length == 2 && p[0] == String.class && p[1] == String.class) {
                try {
                    m.setAccessible(true);
                    Object r = m.invoke(null, ns, path);
                    if (r != null) return (ResourceLocation) r;
                } catch (Throwable ignored) {}
            }
        }
        throw new IllegalStateException("Cannot create ResourceLocation");
    }
}