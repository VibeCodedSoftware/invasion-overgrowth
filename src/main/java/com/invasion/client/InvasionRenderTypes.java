package com.invasion.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;

/** Render type for the green starfield portal surface. Extends RenderType only to reach its protected state shards. */
public final class InvasionRenderTypes extends RenderType {
    static ShaderInstance portalShader;

    private static final RenderType PORTAL = create("invasion_portal", DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> portalShader))
                    .setTextureState(MultiTextureStateShard.builder()
                            .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                            .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                            .build())
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    public static RenderType portal() {
        return PORTAL;
    }

    private InvasionRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
