package com.invasion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hair that grows out of flesh (or dirt), like grass. */
public class FleshPlantBlock extends BushBlock {
    public static final MapCodec<FleshPlantBlock> CODEC = simpleCodec(FleshPlantBlock::new);
    private static final VoxelShape SHAPE = BushBlock.box(2, 0, 2, 14, 13, 14);

    public FleshPlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(com.invasion.InvasionTags.FLESH) || state.is(BlockTags.DIRT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
