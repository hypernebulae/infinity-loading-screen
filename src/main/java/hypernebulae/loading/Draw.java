package hypernebulae.loading;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

final class Draw {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Affine IDENTITY = new Affine();

    static ResourceLocation texture(Minecraft mc, String path) {
        if (CACHE.containsKey(path)) return CACHE.get(path);
        ResourceLocation id = null;
        try (InputStream in = Draw.class.getResourceAsStream("/assets/loading/" + path)) {
            if (in != null) {
                id = new ResourceLocation("loading", path);
                mc.getTextureManager().register(id, new DynamicTexture(NativeImage.read(in)));
            } else {
                System.err.println("[loading] missing texture in jar: " + path);
            }
        } catch (Throwable t) {
            System.err.println("[loading] failed to load " + path + ": " + t);
            id = null;
        }
        CACHE.put(path, id);
        return id;
    }

    static void fill(float x0, float y0, float x1, float y1, int argb) { color(IDENTITY, x0, y0, x1, y1, argb); }

    static void color(Affine t, float x0, float y0, float x1, float y1, int argb) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        if (a == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator ts = Tesselator.getInstance();
        BufferBuilder bb = ts.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(t.x(x0, y1), t.y(x0, y1), 0).color(r, g, b, a).endVertex();
        bb.vertex(t.x(x1, y1), t.y(x1, y1), 0).color(r, g, b, a).endVertex();
        bb.vertex(t.x(x1, y0), t.y(x1, y0), 0).color(r, g, b, a).endVertex();
        bb.vertex(t.x(x0, y0), t.y(x0, y0), 0).color(r, g, b, a).endVertex();
        ts.end();
        RenderSystem.disableBlend();
    }

    static void tex(Affine t, ResourceLocation tex, float x0, float y0, float x1, float y1, int alpha) {
        if (tex == null || alpha <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Tesselator ts = Tesselator.getInstance();
        BufferBuilder bb = ts.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(t.x(x0, y1), t.y(x0, y1), 0).uv(0, 1).color(255, 255, 255, alpha).endVertex();
        bb.vertex(t.x(x1, y1), t.y(x1, y1), 0).uv(1, 1).color(255, 255, 255, alpha).endVertex();
        bb.vertex(t.x(x1, y0), t.y(x1, y0), 0).uv(1, 0).color(255, 255, 255, alpha).endVertex();
        bb.vertex(t.x(x0, y0), t.y(x0, y0), 0).uv(0, 0).color(255, 255, 255, alpha).endVertex();
        ts.end();
        RenderSystem.disableBlend();
    }

    static void texCentered(Affine t, ResourceLocation tex, float cx, float cy, float w, float h, int alpha) {
        tex(t, tex, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, alpha);
    }

    static void progressBar(Affine t, int x0, int y0, int x1, int y1, float progress, float alpha, boolean dark) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24;
        int m = (int) Math.ceil((x1 - x0 - 1) * progress);
        int border = dark ? 0xFFFFFF : 0x000000;
        color(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, a | border);
        color(t, x0, y0, x1, y1, a | (dark ? 0x000000 : 0xFFFFFF));
        color(t, x0 + 1, y0 + 1, x0 + m, y1 - 1, a | 0xE22837);
    }
}