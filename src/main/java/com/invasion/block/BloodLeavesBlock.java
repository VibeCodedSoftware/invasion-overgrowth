package com.invasion.block;

import com.invasion.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Blood Oak leaves: drift falling blood-pink leaves underneath, like cherry leaves. */
public class BloodLeavesBlock extends LeavesBlock {
    public BloodLeavesBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(12) != 0) return;
        BlockPos below = pos.below();
        if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) return;
        level.addParticle(ModRegistry.FALLING_BLOOD_LEAF.get(), pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
    }
}
