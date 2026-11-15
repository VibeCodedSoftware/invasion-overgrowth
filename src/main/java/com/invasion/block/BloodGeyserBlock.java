package com.invasion.block;

import com.invasion.BloodResistance;
import com.invasion.ModRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.invasion.entity.GloveEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A vent of boiling blood: a constant column of red smoke, spurts of blood, and it burns anything that walks on it. */
public class BloodGeyserBlock extends Block {
    public BloodGeyserBlock(Properties props) {
        super(props);
    }

    /**
     * Stepping on a geyser blasts you ~16 blocks into the air. Without Blood Resistance boots it also burns;
     * with them you're unharmed and drift down on slow falling, which makes geysers perfect elevators.
     * Sneak to stand on one safely.
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully() && entity.getDeltaMovement().y < 0.5) {
            boolean protectedByBoots = entity instanceof LivingEntity living && BloodResistance.has(living);
            entity.setDeltaMovement(entity.getDeltaMovement().x * 0.4, 1.75, entity.getDeltaMovement().z * 0.4);
            entity.hurtMarked = true;
            entity.resetFallDistance();
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 1.0F, 0.6F);
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 0.5F);
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(GloveEffects.BLOOD_DROP, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.3);
                }
                if (entity instanceof LivingEntity living) {
                    if (protectedByBoots) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
                    } else if (!entity.fireImmune()) {
                        living.hurt(level.damageSources().hotFloor(), 2.0F);
                        entity.igniteForSeconds(3.0F);
                    }
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (level.isEmptyBlock(pos.above())) {
            for (int i = 0; i < 2; i++) {
                level.addAlwaysVisibleParticle(ModRegistry.BLOOD_SMOKE.get(), true,
                        x + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1), y + random.nextDouble() * 0.3,
                        z + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1), 0.0, 0.07, 0.0);
            }
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 0.6F + random.nextFloat() * 0.3F, false);
            for (int i = 0; i < 14; i++) {
                level.addParticle(GloveEffects.BLOOD_DROP, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4,
                        (random.nextDouble() - 0.5) * 0.25, 0.5 + random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.25);
            }
        }
        if (random.nextInt(100) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.5F, 0.6F, false);
        }
    }
}
