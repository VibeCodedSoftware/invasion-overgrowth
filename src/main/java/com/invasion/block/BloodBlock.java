package com.invasion.block;

import com.invasion.entity.InvaderZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Blood saps the living (hunger + slowness) and mends invaders that wade through it. */
public class BloodBlock extends LiquidBlock {
    public BloodBlock(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity.tickCount % 20 != 0) return;
        if (living instanceof InvaderZombie) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        } else if (living instanceof Player && !com.invasion.BloodResistance.has(living)) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
    }
}
