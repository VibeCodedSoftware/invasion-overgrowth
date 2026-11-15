package com.invasion.block;

import com.invasion.ModFlesh;
import com.invasion.entity.FleshlingEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dropped by the Flesh Thing. Place it and it slowly pulses through three stages, then hatches into a
 * loyal Fleshling pet for the nearest player.
 */
public class FleshlingEggBlock extends Block {
    public static final MapCodec<FleshlingEggBlock> CODEC = simpleCodec(FleshlingEggBlock::new);
    public static final IntegerProperty HATCH = IntegerProperty.create("hatch", 0, 2);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 10, 12);

    public FleshlingEggBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.6F + random.nextFloat() * 0.2F);
        int stage = state.getValue(HATCH);
        if (stage < 2) {
            level.setBlock(pos, state.setValue(HATCH, stage + 1), 2);
            return;
        }
        Player owner = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 32, false);
        FleshlingEntity pet = ModFlesh.FLESHLING.get().create(level);
        if (pet == null) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        pet.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360F, 0);
        if (owner != null) pet.tameTo(owner);
        level.addFreshEntity(pet);
        level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(com.invasion.entity.GloveEffects.BLOOD_DROP, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.7, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
