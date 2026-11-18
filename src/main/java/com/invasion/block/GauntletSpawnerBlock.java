package com.invasion.block;

import com.invasion.InvasionMod;
import com.invasion.ModRegistry;
import com.invasion.entity.TempleGuardian;
import com.invasion.world.InvasionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A mossy pedestal with a glowing top that shows one item floating above it. Use an item on it to place it,
 * use it empty-handed to take the item back. Placing the Particle Gauntlet on one inside the Overgrowth temple
 * wakes the Temple Guardian for a rematch (the gauntlet stays on the pedestal: take it back to fight).
 */
public class GauntletSpawnerBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(3, 3, 3, 13, 13, 13),
            Block.box(2, 13, 2, 14, 16, 14));
    /** How close to the temple centre the pedestal must stand to wake the guardian. */
    private static final int TEMPLE_RANGE = 40;

    public GauntletSpawnerBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GauntletSpawnerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GauntletSpawnerBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!be.item().isEmpty() || stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack placed = player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
            be.setItem(placed);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (placed.is(ModRegistry.PARTICLE_GLOVE.get()) && level instanceof ServerLevel server) tryWakeGuardian(server, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GauntletSpawnerBlockEntity be) || be.item().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack item = be.item();
            be.setItem(ItemStack.EMPTY);
            if (!player.addItem(item)) player.drop(item, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void tryWakeGuardian(ServerLevel level, BlockPos pos, Player player) {
        InvasionData data = InvasionData.get(level.getServer());
        BlockPos temple = data.templeCenter();
        boolean inTemple = level.dimension() == InvasionMod.OVERGROWTH && temple != null
                && Math.abs(pos.getX() - temple.getX()) <= TEMPLE_RANGE && Math.abs(pos.getZ() - temple.getZ()) <= TEMPLE_RANGE;
        if (!inTemple) return; // anywhere else it is just a display
        if (!level.getEntitiesOfClass(TempleGuardian.class, new AABB(temple).inflate(96)).isEmpty()) {
            player.displayClientMessage(Component.literal("The Temple Guardian is already awake.").withStyle(ChatFormatting.RED), true);
            return;
        }
        TempleGuardian boss = ModRegistry.GUARDIAN.get().create(level);
        if (boss == null) return;
        boss.moveTo(temple.getX() + 0.5, temple.getY() + 1, temple.getZ() + 0.5, 180F, 0F);
        boss.setRematch(true);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, boss.getX(), boss.getY() + 1.5, boss.getZ(), 60, 1.2, 1.5, 1.2, 0.1);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.6F);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 120 * 120) {
                p.displayClientMessage(Component.literal("The Temple Guardian rises again!").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), true);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 1.02,
                    pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.02 + r.nextDouble() * 0.02, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GauntletSpawnerBlockEntity be && !be.item().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, be.item());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
