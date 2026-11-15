package com.invasion.client;

import com.invasion.ModRegistry;
import com.invasion.block.InvasionPortalBlock;
import com.invasion.block.PortalCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Draws each portal block as a thin, nether-portal-style pane of green starfield. */
public class PortalRenderer implements BlockEntityRenderer<PortalCoreBlockEntity> {
    private static final float THIN0 = 6 / 16F, THIN1 = 10 / 16F;

    public PortalRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PortalCoreBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        boolean alongX = !state.hasProperty(InvasionPortalBlock.AXIS) || state.getValue(InvasionPortalBlock.AXIS) == Direction.Axis.X;
        float x0 = alongX ? 0 : THIN0, x1 = alongX ? 1 : THIN1, z0 = alongX ? THIN0 : 0, z1 = alongX ? THIN1 : 1;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(InvasionRenderTypes.portal());
        for (Direction d : Direction.values()) {
            boolean bigFace = alongX ? d.getAxis() == Direction.Axis.Z : d.getAxis() == Direction.Axis.X;
            if (!bigFace && level.getBlockState(pos.relative(d)).is(ModRegistry.PORTAL.get())) continue;
            switch (d) {
                case SOUTH -> quad(vc, m, x0, 0, z1, x1, 0, z1, x1, 1, z1, x0, 1, z1);
                case NORTH -> quad(vc, m, x0, 1, z0, x1, 1, z0, x1, 0, z0, x0, 0, z0);
                case EAST -> quad(vc, m, x1, 1, z0, x1, 1, z1, x1, 0, z1, x1, 0, z0);
                case WEST -> quad(vc, m, x0, 0, z0, x0, 0, z1, x0, 1, z1, x0, 1, z0);
                case DOWN -> quad(vc, m, x0, 0, z0, x1, 0, z0, x1, 0, z1, x0, 0, z1);
                case UP -> quad(vc, m, x0, 1, z1, x1, 1, z1, x1, 1, z0, x0, 1, z0);
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.addVertex(m, ax, ay, az);
        vc.addVertex(m, bx, by, bz);
        vc.addVertex(m, cx, cy, cz);
        vc.addVertex(m, dx, dy, dz);
    }
}
