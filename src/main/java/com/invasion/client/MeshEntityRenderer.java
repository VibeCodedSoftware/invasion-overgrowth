package com.invasion.client;

import com.invasion.entity.FleshThingEntity;
import com.invasion.entity.FleshlingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Renders an artist mesh (see {@link MeshModel}) with simple procedural animation: walking legs, bobbing heads. */
public class MeshEntityRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    private final String mesh;

    private float scale = 1.0F;

    public MeshEntityRenderer(EntityRendererProvider.Context ctx, String mesh, float shadow) {
        super(ctx);
        this.mesh = mesh;
        this.shadowRadius = shadow;
    }

    public MeshEntityRenderer(EntityRendererProvider.Context ctx, String mesh, float shadow, float scale) {
        this(ctx, mesh, shadow);
        this.scale = scale;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        MeshModel model = MeshModel.get(mesh);
        if (model.quads.isEmpty()) return;
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (entity.deathTime > 0) {
            float f = Mth.sqrt(Math.min(1.0F, (entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F));
            pose.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }
        float squash = entity instanceof FleshlingEntity f && f.dormantTicks() > 0 ? 0.45F : 1.0F;
        pose.scale(scale / 16F, scale * squash / 16F, scale / 16F);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);

        float limb = entity.walkAnimation.position(partialTick);
        float limbAmount = Math.min(1.0F, entity.walkAnimation.speed(partialTick));
        float age = entity.tickCount + partialTick;
        float headLift = 0;
        if (entity instanceof FleshThingEntity thing) {
            switch (thing.phase()) {
                case STOMP_WINDUP -> headLift = -0.5F;
                case SPIT_WINDUP, SPIT -> headLift = 0.35F;
                case CHARGE, CHARGE_WINDUP -> headLift = 0.25F;
                case EXHAUSTED -> headLift = 0.6F;
                default -> {}
            }
        }

        int nBones = model.pivots.size();
        float[] angle = new float[nBones];
        for (int b = 0; b < nBones; b++) {
            switch (model.roles.get(b)) {
                case LEG -> angle[b] = Mth.sin(limb * 0.6F + (model.legIndex[b] % 2 == 0 ? 0 : Mth.PI)) * 0.55F * limbAmount;
                case HEAD -> angle[b] = Mth.sin(age * 0.09F + b) * 0.07F + headLift;
                default -> angle[b] = 0;
            }
        }

        PoseStack.Pose last = pose.last();
        for (int t = 0; t < model.textures.size(); t++) {
            ResourceLocation tex = model.textures.get(t);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
            for (MeshModel.Quad q : model.quads) {
                if (q.texture() != t) continue;
                float a = q.bone() < nBones ? angle[q.bone()] : 0;
                float[] piv = q.bone() < nBones ? model.pivots.get(q.bone()) : new float[3];
                float cos = Mth.cos(a), sin = Mth.sin(a);
                float[] n = q.normal();
                float ny = n[1] * cos - n[2] * sin, nz = n[1] * sin + n[2] * cos;
                for (float[] v : q.v()) {
                    float y = v[1] - piv[1], z = v[2] - piv[2];
                    float ry = y * cos - z * sin + piv[1], rz = y * sin + z * cos + piv[2];
                    vc.addVertex(last, v[0], ry, rz).setColor(-1).setUv(v[3], v[4]).setOverlay(overlay).setLight(light).setNormal(last, n[0], ny, nz);
                }
            }
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        MeshModel model = MeshModel.get(mesh);
        return model.textures.isEmpty() ? ResourceLocation.withDefaultNamespace("textures/misc/white.png") : model.textures.get(0);
    }
}
