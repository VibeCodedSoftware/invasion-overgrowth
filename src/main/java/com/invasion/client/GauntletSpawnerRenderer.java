package com.invasion.client;

import com.invasion.block.GauntletSpawnerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the displayed item floating, bobbing and slowly turning above the glowing top, lit by the glow. */
public class GauntletSpawnerRenderer implements BlockEntityRenderer<GauntletSpawnerBlockEntity> {
    public GauntletSpawnerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(GauntletSpawnerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack item = be.item();
        if (item.isEmpty() || be.getLevel() == null) return;
        float time = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.35 + Mth.sin(time / 12.0F) * 0.06, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F % 360));
        pose.scale(0.75F, 0.75F, 0.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }
}
