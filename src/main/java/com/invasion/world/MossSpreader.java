package com.invasion.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Creeps vanilla moss outward from a portal frame. */
public final class MossSpreader {

    public static void spread(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int attempt = 0; attempt < 2; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) continue;
            BlockPos column = center.offset(dx, 0, dz);
            if (!level.isLoaded(column)) continue;

            // Walk down to the top surface near the portal's height.
            for (int dy = 8; dy >= -10; dy--) {
                BlockPos p = column.above(dy);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s.canBeReplaced()) continue;
                BlockState converted = convert(s);
                if (converted != null) {
                    level.setBlock(p, converted, 3);
                    BlockPos above = p.above();
                    if (converted.is(Blocks.MOSS_BLOCK) && level.getBlockState(above).isAir() && random.nextInt(4) == 0) {
                        level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), 3);
                    }
                } else if (s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSSY_COBBLESTONE)) {
                    // Already mossy: creep onto a side face of the block below for a draping look.
                    BlockPos side = p.below();
                    BlockState sideState = level.getBlockState(side);
                    BlockState sideConv = convert(sideState);
                    if (sideConv != null && random.nextInt(3) == 0) level.setBlock(side, sideConv, 3);
                }
                break;
            }
        }
    }

    @Nullable
    private static BlockState convert(BlockState s) {
        if (s.is(Blocks.MOSS_BLOCK)) return null;
        if (s.is(Blocks.COBBLESTONE)) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (s.is(Blocks.STONE_BRICKS)) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (s.is(Blocks.COBBLESTONE_WALL)) return Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState();
        if (s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.FARMLAND) || s.is(Blocks.DIRT_PATH)) {
            return Blocks.MOSS_BLOCK.defaultBlockState();
        }
        return null;
    }

    private MossSpreader() {}
}
