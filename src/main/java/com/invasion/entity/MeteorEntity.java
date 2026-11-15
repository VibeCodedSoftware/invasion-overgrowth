package com.invasion.entity;

import com.invasion.ModRegistry;
import com.invasion.world.InvasionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A giant tumbling block of bloody cobblestone that streaks across the sky and opens the portal. */
public class MeteorEntity extends Entity {
    private static final double SPEED = 2.0;
    private static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.1F), 3.0F);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.3F), 2.5F);

    private Vec3 target = Vec3.ZERO;
    private boolean hasTarget;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public MeteorEntity(Level level, Vec3 start, Vec3 target) {
        this(ModRegistry.METEOR.get(), level);
        setPos(start);
        this.target = target;
        this.hasTarget = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide) {
            spawnTrail(level, level.random);
            return;
        }
        if (!hasTarget) {
            discard();
            return;
        }
        Vec3 toTarget = target.subtract(position());
        double distance = toTarget.length();
        if (distance <= SPEED || tickCount > 1200) {
            InvasionManager.onMeteorImpact((ServerLevel) level, BlockPos.containing(target));
            discard();
            return;
        }
        Vec3 velocity = toTarget.scale(SPEED / distance);
        setDeltaMovement(velocity);
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        if (tickCount % 15 == 1) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 12.0F, 0.4F);
        }
    }

    private void spawnTrail(Level level, RandomSource r) {
        double vx = getX() - xo, vy = getY() - yo, vz = getZ() - zo;
        for (int i = 0; i < 12; i++) {
            double f = r.nextDouble();
            double x = getX() - vx * f + (r.nextDouble() - 0.5) * 3;
            double y = getY() + 1.5 - vy * f + (r.nextDouble() - 0.5) * 3;
            double z = getZ() - vz * f + (r.nextDouble() - 0.5) * 3;
            level.addAlwaysVisibleParticle(i % 3 == 0 ? YELLOW : i % 3 == 1 ? ORANGE : ParticleTypes.FLAME, true,
                    x, y, z, -vx * 0.05, -vy * 0.05, -vz * 0.05);
        }
        level.addAlwaysVisibleParticle(ParticleTypes.LAVA, true, getX(), getY() + 1.5, getZ(), 0, 0, 0);
        level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, getX(), getY() + 1.5, getZ(), 0, 0.02, 0);
        // Long-lasting smoke column that stays in the sky as the trail.
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                getX() + (r.nextDouble() - 0.5), getY() + 1.5, getZ() + (r.nextDouble() - 0.5), 0, 0.01, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        hasTarget = tag.contains("TargetX");
        target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (hasTarget) {
            tag.putDouble("TargetX", target.x);
            tag.putDouble("TargetY", target.y);
            tag.putDouble("TargetZ", target.z);
        }
    }
}
