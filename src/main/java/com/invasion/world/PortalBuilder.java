package com.invasion.world;

import com.invasion.ModRegistry;
import com.invasion.block.InvasionPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class PortalBuilder {

    /**
     * Builds a vertical 4x5 bloody cobblestone frame (2x3 opening) running along the X axis.
     * {@code core} is the bottom-left opening block; players enter from the north or south side.
     */
    public static void buildPortal(ServerLevel level, BlockPos core) {
        BlockState frame = ModRegistry.PORTAL_FRAME.get().defaultBlockState();
        BlockState portal = ModRegistry.PORTAL.get().defaultBlockState().setValue(InvasionPortalBlock.AXIS, Direction.Axis.X);

        // Clear a space in front of and behind the portal, and lay a walkable floor.
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    BlockPos p = core.offset(dx, dy, dz);
                    if (level.getBlockState(p).getDestroySpeed(level, p) >= 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
                BlockPos floor = core.offset(dx, -1, dz);
                BlockState fs = level.getBlockState(floor);
                if (fs.canBeReplaced() || !fs.getFluidState().isEmpty()) {
                    level.setBlock(floor, ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState(), 3);
                }
            }
        }
        // Unbreakable foundation so nobody can dig the portal out from below.
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(core.offset(dx, -2, dz), frame, 3);
            }
        }
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                BlockPos p = core.offset(dx, dy, 0);
                boolean edge = dx == -1 || dx == 2 || dy == -1 || dy == 3;
                if (edge) level.setBlock(p, frame, 3);
            }
        }
        // Fill the opening last so neighbour updates never see a half-built frame.
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                boolean isCore = dx == 0 && dy == 0;
                level.setBlock(core.offset(dx, dy, 0), portal.setValue(InvasionPortalBlock.CORE, isCore), 2);
            }
        }
    }

    /** Blasts a crater where the meteor hit, throws debris, and leaves the portal at the bottom. Returns the portal core. */
    public static BlockPos meteorImpact(ServerLevel level, BlockPos ground, RandomSource random) {
        int radius = 9;
        int surfaceY = ground.getY() - 1;
        double cx = ground.getX() + 0.5, cz = ground.getZ() + 0.5;

        level.explode(null, cx, surfaceY + 1.5, cz, 6.0F, true, Level.ExplosionInteraction.TNT);

        // Fling some of the ground into the air like the original.
        for (int i = 0; i < 40; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.offset(dx, 0, dz)).below();
            BlockState s = level.getBlockState(top);
            if (s.isAir() || s.getDestroySpeed(level, top) < 0 || s.hasBlockEntity() || !s.getFluidState().isEmpty()) continue;
            FallingBlockEntity debris = FallingBlockEntity.fall(level, top, s);
            double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
            debris.setDeltaMovement(dx / len * (0.4 + random.nextDouble() * 0.6), 0.8 + random.nextDouble() * 0.9,
                    dz / len * (0.4 + random.nextDouble() * 0.6));
            debris.dropItem = false;
            debris.hurtMarked = true;
        }

        // Carve a bowl and line it with meteor rock and scorched ground.
        int centerDepth = Math.round(radius * 0.55F);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                int depth = (int) Math.round(Math.sqrt(radius * radius - dist * dist) * 0.55);
                int bottom = surfaceY - depth;
                for (int y = bottom + 1; y <= surfaceY + 10; y++) {
                    BlockPos p = new BlockPos(ground.getX() + dx, y, ground.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getDestroySpeed(level, p) >= 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                BlockPos b = new BlockPos(ground.getX() + dx, bottom, ground.getZ() + dz);
                if (level.getBlockState(b).getDestroySpeed(level, b) < 0) continue;
                float roll = random.nextFloat();
                BlockState lining = roll < 0.35F ? ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState()
                        : roll < 0.43F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : roll < 0.70F ? Blocks.COARSE_DIRT.defaultBlockState()
                        : roll < 0.87F ? Blocks.COBBLESTONE.defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState();
                level.setBlock(b, lining, 3);
                if (dist > 4 && random.nextFloat() < 0.07F) {
                    level.setBlock(b.above(), BaseFireBlock.getState(level, b.above()), 3);
                }
            }
        }

        // Chunks of the meteor itself scattered around the rim.
        for (int i = 0; i < 7; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            int dist = radius + 1 + random.nextInt(5);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ground.offset(Math.round(Mth.cos(angle) * dist), 0, Math.round(Mth.sin(angle) * dist)));
            int size = 1 + random.nextInt(2);
            for (int x = -size; x <= size; x++)
                for (int y = -1; y <= size; y++)
                    for (int z = -size; z <= size; z++)
                        if (x * x + y * y + z * z <= size * size + random.nextInt(2)) {
                            BlockPos p = at.offset(x, y, z);
                            if (level.getBlockState(p).getDestroySpeed(level, p) >= 0)
                                level.setBlock(p, ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState(), 3);
                        }
        }

        BlockPos core = new BlockPos(ground.getX(), surfaceY - centerDepth + 1, ground.getZ());
        buildPortal(level, core);
        return core;
    }

    private PortalBuilder() {}
}
