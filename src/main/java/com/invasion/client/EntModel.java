package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.entity.EntEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Tall tree-giant: root feet, long arms ending in dangling roots, a protruding nose, and a crown anchor for leaf blocks. */
public class EntModel extends HierarchicalModel<EntEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(InvasionMod.id("ent"), "main");

    private final ModelPart root, torso, head, crown, rightArm, leftArm, rightLeg, leftLeg;

    public EntModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.crown = head.getChild("crown");
        this.rightArm = torso.getChild("right_arm");
        this.leftArm = torso.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // ground is y = 24, hips at y = -10 (34 units up)
        for (int side : new int[]{-1, 1}) {
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                            .texOffs(0, 58).addBox(-3, 0, -3, 6, 14, 6)
                            .texOffs(24, 58).addBox(-3.5F, 14, -3.5F, 7, 16, 7)
                            .texOffs(0, 81).addBox(-4, 30, -8, 8, 4, 11),
                    PartPose.offset(side * 5, -10, 0));
        }
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8, -26, -4.5F, 16, 26, 9)
                        .texOffs(94, 10).addBox(-4, -32, -4, 8, 6, 8)
                        .texOffs(50, 21).addBox(-15, -27, -4, 8, 7, 8)
                        .texOffs(50, 21).addBox(7, -27, -4, 8, 7, 8),
                PartPose.offset(0, -10, 0));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(50, 0).addBox(-6, -11, -5, 12, 11, 10)
                        .texOffs(94, 0).addBox(-1.5F, -7, -8, 3, 7, 3),
                PartPose.offset(0, -30, 0));
        head.addOrReplaceChild("crown", CubeListBuilder.create(), PartPose.offset(0, -11, 0));
        for (int side : new int[]{-1, 1}) {
            torso.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                            .texOffs(82, 24).addBox(-3, 0, -3, 6, 20, 6)
                            .texOffs(0, 35).addBox(-3.5F, 18, -3.5F, 7, 16, 7)
                            .texOffs(28, 35).addBox(-4.5F, 34, -4.5F, 9, 4, 9)
                            .texOffs(64, 50).addBox(-4.5F, 38, -4.5F, 9, 14, 9),
                    PartPose.offset(side * 11, -22, 0));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public ModelPart torso() { return torso; }
    public ModelPart head() { return head; }
    public ModelPart crown() { return crown; }

    @Override
    public void setupAnim(EntEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 0.35F) * 0.7F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.6F + Mth.sin(ageInTicks * 0.05F) * 0.04F;
        leftArm.xRot = swing * 0.6F - Mth.sin(ageInTicks * 0.05F) * 0.04F;
        rightArm.zRot = 0.05F;
        leftArm.zRot = -0.05F;
        int attack = entity.getAttackAnimationTick();
        if (attack > 0) {
            float a = Mth.sin((attack / 10.0F) * Mth.PI) * 1.6F;
            rightArm.xRot = -a;
            leftArm.xRot = -a;
        }
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        head.zRot = Mth.sin(ageInTicks * 0.03F) * 0.03F;
    }
}
