package com.invasion.client;

import com.invasion.InvasionMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity skins at runtime from the player's own installed block textures (mossy cobblestone,
 * oak/birch log bark) plus a small overlay that ships with the mod. Nothing from Mojang is redistributed,
 * and resource packs automatically restyle the mobs. Overlay pixels: transparent = keep the block texture,
 * pure magenta (#FF00FF) = cut out, anything else is painted on top.
 */
public final class RuntimeSkins implements ResourceManagerReloadListener {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final int CUT = 0xFF00FF;

    public static ResourceLocation get(String key, ResourceLocation blockTexture, ResourceLocation overlay) {
        ResourceLocation id = CACHE.get(key);
        if (id == null) {
            id = build(key, blockTexture, overlay);
            CACHE.put(key, id);
        }
        return id;
    }

    private static ResourceLocation build(String key, ResourceLocation blockTexture, ResourceLocation overlay) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage base = read(mc.getResourceManager(), blockTexture); NativeImage over = read(mc.getResourceManager(), overlay)) {
            int w = over.getWidth(), h = over.getHeight();
            int bw = base.getWidth(), bh = Math.min(base.getHeight(), bw); // first frame only for animated textures
            NativeImage out = new NativeImage(w, h, true);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int o = over.getPixelRGBA(x, y);           // packed ABGR
                    int a = o >>> 24;
                    if (a != 0 && (o & 0xFFFFFF) == CUT) { out.setPixelRGBA(x, y, 0); continue; }
                    int bx = (x % 16) * bw / 16, by = (y % 16) * bh / 16;
                    int b = base.getPixelRGBA(bx, by) | 0xFF000000;
                    if (a == 0) { out.setPixelRGBA(x, y, b); continue; }
                    float f = a / 255.0F;
                    int r = (int) ((o & 255) * f + (b & 255) * (1 - f));
                    int g = (int) (((o >> 8) & 255) * f + ((b >> 8) & 255) * (1 - f));
                    int bl = (int) (((o >> 16) & 255) * f + ((b >> 16) & 255) * (1 - f));
                    out.setPixelRGBA(x, y, 0xFF000000 | (bl << 16) | (g << 8) | r);
                }
            }
            ResourceLocation id = InvasionMod.id("runtime/" + key);
            mc.getTextureManager().register(id, new DynamicTexture(out));
            return id;
        } catch (IOException e) {
            InvasionMod.LOGGER.warn("Could not build runtime skin {}: {}", key, e.toString());
            return overlay;
        }
    }

    /** Registers a recoloured copy of a vanilla texture under {@code target} (used for the Blood Oak boat). */
    static void tinted(ResourceLocation target, ResourceLocation source, int rgb) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage src = read(mc.getResourceManager(), source)) {
            NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), true);
            int tr = (rgb >> 16) & 255, tg = (rgb >> 8) & 255, tb = rgb & 255;
            for (int x = 0; x < src.getWidth(); x++) for (int y = 0; y < src.getHeight(); y++) {
                int p = src.getPixelRGBA(x, y);
                int r = (p & 255) * tr / 255, g = ((p >> 8) & 255) * tg / 255, b = ((p >> 16) & 255) * tb / 255;
                out.setPixelRGBA(x, y, (p & 0xFF000000) | (b << 16) | (g << 8) | r);
            }
            mc.getTextureManager().register(target, new DynamicTexture(out));
        } catch (IOException e) {
            InvasionMod.LOGGER.warn("Could not build tinted texture {}: {}", target, e.toString());
        }
    }

    /** The Blood Oak planks palette, dark to light. */
    private static final int[] PLANK_RAMP = {0x2a0b0b, 0x4a1716, 0x5e1f1d, 0x722825, 0x86322d, 0x983c35, 0xa84840, 0xc06058};

    /** Re-shades a vanilla texture onto the Blood Oak plank palette by brightness (keeps its shading, swaps its colours). */
    static void gradientMapped(ResourceLocation target, ResourceLocation source) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage src = read(mc.getResourceManager(), source)) {
            int lo = 255, hi = 0;
            for (int x = 0; x < src.getWidth(); x++) for (int y = 0; y < src.getHeight(); y++) {
                int p = src.getPixelRGBA(x, y); if ((p >>> 24) == 0) continue;
                int l = ((p & 255) + ((p >> 8) & 255) + ((p >> 16) & 255)) / 3; lo = Math.min(lo, l); hi = Math.max(hi, l);
            }
            NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), true);
            for (int x = 0; x < src.getWidth(); x++) for (int y = 0; y < src.getHeight(); y++) {
                int p = src.getPixelRGBA(x, y);
                if ((p >>> 24) == 0) { out.setPixelRGBA(x, y, 0); continue; }
                int l = ((p & 255) + ((p >> 8) & 255) + ((p >> 16) & 255)) / 3;
                float t = hi > lo ? (l - lo) / (float) (hi - lo) : 0.5F;
                int c = PLANK_RAMP[Math.min(PLANK_RAMP.length - 1, (int) (t * PLANK_RAMP.length))];
                out.setPixelRGBA(x, y, (p & 0xFF000000) | ((c & 255) << 16) | (c & 0xFF00) | ((c >> 16) & 255));
            }
            mc.getTextureManager().register(target, new DynamicTexture(out));
        } catch (IOException e) {
            InvasionMod.LOGGER.warn("Could not build Blood Oak texture {}: {}", target, e.toString());
        }
    }

    private static NativeImage read(ResourceManager rm, ResourceLocation loc) throws IOException {
        Resource r = rm.getResourceOrThrow(loc);
        try (InputStream in = r.open()) {
            return NativeImage.read(in);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation id : CACHE.values()) {
            if (id.getPath().startsWith("runtime/")) mc.getTextureManager().release(id);
        }
        CACHE.clear();
        MeshModel.clearCache();
        gradientMapped(InvasionMod.id("textures/entity/boat/blood_oak.png"), ResourceLocation.withDefaultNamespace("textures/entity/boat/birch.png"));
        gradientMapped(InvasionMod.id("textures/entity/chest_boat/blood_oak.png"), ResourceLocation.withDefaultNamespace("textures/entity/chest_boat/birch.png"));
    }
}
