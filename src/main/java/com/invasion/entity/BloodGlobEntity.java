package com.invasion.entity;

import com.invasion.BloodResistance;
import com.invasion.ModFlesh;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A lump of flesh spat by the Flesh Thing: hurts, and saps the target with hunger and slowness. */
public class BloodGlobEntity extends ThrowableItemProjectile {
    public BloodGlobEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public BloodGlobEntity(Level level, LivingEntity shooter) {
        super(ModFlesh.BLOOD_GLOB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModFlesh.FLESH_ITEM.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) level().addParticle(GloveEffects.BLOOD_DROP, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() instanceof LivingEntity target && !(target instanceof FleshlingEntity) && !(target instanceof FleshThingEntity)) {
            target.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), 6.0F);
            if (!BloodResistance.has(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(GloveEffects.BLOOD_DROP, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
            discard();
        }
    }
}
