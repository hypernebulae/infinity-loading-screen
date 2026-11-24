package hypernebulae.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class Compat {
    private static Field screenField;
    private static Method screenMethod;
    private static Object pipeline;
    private static Method blitMethod;
    private static boolean blitFailed;

    static Screen screen(Minecraft mc) {
        try {
            if (screenField == null && screenMethod == null) {
                try {
                    screenField = Minecraft.class.getDeclaredField("screen");
                    screenField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    screenMethod = mc.gui.getClass().getMethod("screen");
                }
            }
            return (Screen) (screenField != null ? screenField.get(mc) : screenMethod.invoke(mc.gui));
        } catch (Throwable t) {
            return null;
        }
    }

    static void initScreen(Minecraft mc, Screen s, int w, int h) {
        for (Method m : Screen.class.getMethods()) {
            if (!m.getName().equals("init")) continue;
            Class<?>[] p = m.getParameterTypes();
            try {
                if (p.length == 3 && p[0] == Minecraft.class) { m.invoke(s, mc, w, h); return; }
                if (p.length == 2 && p[0] == int.class) { m.invoke(s, w, h); return; }
            } catch (Throwable t) { System.err.println("[loading] screen init failed: " + t); }
        }
    }

    static void renderScreen(Screen s, Object g, float pt) {
        for (Method m : Screen.class.getMethods()) {
            if (!m.getName().equals("extractRenderStateWithTooltipAndSubtitles")) continue;
            Class<?>[] p = m.getParameterTypes();
            try {
                if (p.length == 4) { m.invoke(s, g, 0, 0, pt); return; }
                if (p.length == 1) {
                    Runnable r = () -> { try { m.invoke(s, g); } catch (Throwable ignored) {} };
                    for (Method w : g.getClass().getMethods()) {
                        if (w.getName().equals("extractWithoutMouse") && w.getParameterCount() == 1) {
                            w.invoke(g, r);
                            return;
                        }
                    }
                    r.run();
                    return;
                }
            } catch (Throwable t) { System.err.println("[loading] screen render failed: " + t); }
        }
    }

    static void setOverlay(Minecraft mc, Overlay o) {
        for (Object target : new Object[] { mc, mc.gui }) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals("setOverlay") && m.getParameterCount() == 1) {
                    try { m.invoke(target, o); return; } catch (Throwable ignored) {}
                }
            }
        }
    }

    static void blit(Object g, Identifier tex, int x, int y, int w, int h, int argb) {
        if (blitFailed) return;
        try {
            if (blitMethod == null) {
                pipeline = findPipeline();
                for (Method m : g.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("blit") && p.length == 11 && pipeline != null
                            && p[0].isInstance(pipeline) && p[1] == Identifier.class) {
                        blitMethod = m;
                        break;
                    }
                }
                if (blitMethod == null) throw new IllegalStateException("no matching blit overload");
            }
            blitMethod.invoke(g, pipeline, tex, x, y, 0f, 0f, w, h, w, h, argb);
        } catch (Throwable t) {
            blitFailed = true;
            System.err.println("[loading] textures disabled: " + t);
        }
    }

    private static Object findPipeline() {
        String[][] candidates = {
                { "net.minecraft.client.renderer.RenderPipelines", "GUI_TEXTURED" },
                { "net.minecraft.client.gui.GuiPipeline", "TEXTURED" } };
        for (String[] c : candidates) {
            try { return Class.forName(c[0]).getField(c[1]).get(null); } catch (Throwable ignored) {}
        }
        return null;
    }
}