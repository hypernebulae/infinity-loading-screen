package hypernebulae.loading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

final class Draw {
    record Tex(ResourceLocation id, int w, int h) {}

    private static final Map<String, Tex> CACHE = new HashMap<>();

    /** Loads assets/loading/<path> straight from the mod jar. Returns null if missing. */
    static Tex texture(Minecraft mc, String path) {
        if (CACHE.containsKey(path)) return CACHE.get(path);
        Tex tex = null;
        try (InputStream in = Draw.class.getResourceAsStream("/assets/loading/" + path)) {
            if (in != null) {
                NativeImage img = NativeImage.read(in);
                tex = new Tex(Res.make("loading", path), img.getWidth(), img.getHeight());
                mc.getTextureManager().register(tex.id(), new DynamicTexture(img));
            } else {
                System.err.println("[loading] missing texture in jar: " + path);
            }
        } catch (Throwable t) {
            System.err.println("[loading] failed to load " + path + ": " + t);
            tex = null;
        }
        CACHE.put(path, tex);
        return tex;
    }

    /** Draws a texture centred on (cx, cy) in the current pose space. */
    static void blitCentered(GuiGraphics g, Tex t, int cx, int cy, int w, int h, int alpha) {
        if (t == null || alpha <= 0) return;
        g.flush(); // keep fills and textures in draw order
        Blit.draw(g, t, cx - w / 2, cy - h / 2, w, h, Math.min(alpha, 255));
    }

    static void progressBar(GuiGraphics g, int x0, int y0, int x1, int y1,
                            float progress, float alpha, boolean dark) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24;
        int m = (int) Math.ceil((x1 - x0 - 1) * progress);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | (dark ? 0xFFFFFF : 0x000000));
        g.fill(x0, y0, x1, y1, a | (dark ? 0x000000 : 0xFFFFFF));
        g.fill(x0 + 1, y0 + 1, x0 + m, y1 - 1, a | 0xE22837);
    }
}