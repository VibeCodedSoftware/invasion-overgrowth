package com.invasion.entity;

import com.invasion.ModFlesh;
import com.invasion.item.GloveCharge.Element;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Flesh Thing, boss of the Flesh nest. Fights in a readable loop:
 * BITE (chase) -> STOMP (telegraphed rear-up, shockwave) -> REST -> SPIT (three flesh globs) -> REST
 * -> CHARGE (telegraphed bull-rush) -> EXHAUSTED (double damage). Calls Fleshlings at 2/3 and 1/3 health.
 */
public class FleshThingEntity extends Monster {
    public enum Phase { WALK, STOMP_WINDUP, STOMP, REST, SPIT_WINDUP, SPIT, CHARGE_WINDUP, CHARGE, EXHAUSTED }

    private static final Phase[] CYCLE = {Phase.STOMP_WINDUP, Phase.REST, Phase.SPIT_WINDUP, Phase.REST, Phase.CHARGE_WINDUP, Phase.EXHAUSTED};
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(FleshThingEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.invasion.flesh_thing"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
    private int phaseTicks = 80;
    private int cycleIndex;
    private int minionWaves;
    private Vec3 chargeDir = Vec3.ZERO;

    public FleshThingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 300;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createFleshThingAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 0);
    }

    public Phase phase() {
        return Phase.values()[entityData.get(PHASE)];
    }

    private void enter(Phase p, int ticks) {
        entityData.set(PHASE, p.ordinal());
        phaseTicks = ticks;
        setGlowingTag(p == Phase.EXHAUSTED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() { return phase() == Phase.WALK && super.canUse(); }
            @Override public boolean canContinueToUse() { return phase() == Phase.WALK && super.canContinueToUse(); }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, FleshlingEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private void nextInCycle() {
        Phase next = CYCLE[cycleIndex++ % CYCLE.length];
        switch (next) {
            case STOMP_WINDUP -> enter(next, 22);
            case SPIT_WINDUP -> enter(next, 20);
            case CHARGE_WINDUP -> enter(next, 25);
            case REST -> enter(next, 45);
            case EXHAUSTED -> enter(next, 80);
            default -> enter(Phase.WALK, 80);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (phase() != Phase.WALK) enter(Phase.WALK, 80);
            return;
        }

        float hp = getHealth() / getMaxHealth();
        if ((minionWaves == 0 && hp < 0.66F) || (minionWaves == 1 && hp < 0.33F)) {
            minionWaves++;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 1.4F);
            for (int i = 0; i < 3 + minionWaves; i++) {
                FleshlingEntity f = ModFlesh.FLESHLING.get().create(level);
                if (f == null) continue;
                f.moveTo(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, random.nextFloat() * 360, 0);
                f.setTarget(target);
                level.addFreshEntity(f);
                level.sendParticles(GloveEffects.BLOOD_DROP, f.getX(), f.getY() + 0.5, f.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            }
        }

        Phase p = phase();
        boolean stationary = p != Phase.WALK && p != Phase.CHARGE;
        if (stationary) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        if (p != Phase.EXHAUSTED && p != Phase.CHARGE) getLookControl().setLookAt(target, 30, 30);
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();

        switch (p) {
            case WALK -> { if (--phaseTicks <= 0) nextInCycle(); }
            case STOMP_WINDUP -> {
                if (phaseTicks == 22) level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
                if (--phaseTicks <= 0) enter(Phase.STOMP, 6);
            }
            case STOMP -> {
                if (phaseTicks == 6) {
                    GloveEffects.shockwave(level, this, Element.REDSTONE, 7.0, 8.0F, e -> !(e instanceof FleshlingEntity));
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModFlesh.FLESH.get().defaultBlockState()),
                            getX(), getY() + 0.2, getZ(), 60, 2.5, 0.2, 2.5, 0.2);
                }
                if (--phaseTicks <= 0) nextInCycle();
            }
            case SPIT_WINDUP -> {
                level.sendParticles(GloveEffects.BLOOD_DROP, getX(), getEyeY(), getZ(), 4, 0.8, 0.3, 0.8, 0);
                if (phaseTicks == 20) level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 1.0F);
                if (--phaseTicks <= 0) enter(Phase.SPIT, 30);
            }
            case SPIT -> {
                if (phaseTicks % 10 == 0) {
                    BloodGlobEntity glob = new BloodGlobEntity(level, this);
                    glob.setPos(getX(), getEyeY() - 0.3, getZ());
                    Vec3 d = target.getEyePosition().subtract(glob.position());
                    glob.shoot(d.x, d.y + d.horizontalDistance() * 0.15, d.z, 1.3F, 4.0F);
                    level.addFreshEntity(glob);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 2.0F, 0.5F);
                }
                if (--phaseTicks <= 0) nextInCycle();
            }
            case CHARGE_WINDUP -> {
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 3, 1.0, 0.1, 1.0, 0.02);
                if (phaseTicks == 25) level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.7F);
                chargeDir = flat;
                if (--phaseTicks <= 0) enter(Phase.CHARGE, 30);
            }
            case CHARGE -> {
                setDeltaMovement(chargeDir.x * 0.7, getDeltaMovement().y, chargeDir.z * 0.7);
                hurtMarked = true;
                setYRot((float) Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z)));
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5), e -> e != this && !(e instanceof FleshlingEntity))) {
                    e.hurt(damageSources().mobAttack(this), 12.0F);
                    GloveEffects.knock(e, chargeDir, 1.6, 0.5);
                }
                if (horizontalCollision || --phaseTicks <= 0) {
                    if (horizontalCollision) level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.5F);
                    nextInCycle();
                }
            }
            case REST -> {
                if (phaseTicks % 10 == 0) level.sendParticles(ParticleTypes.POOF, getX(), getEyeY(), getZ(), 3, 0.5, 0.2, 0.5, 0.01);
                if (--phaseTicks <= 0) enter(Phase.WALK, 70);
            }
            case EXHAUSTED -> {
                if (phaseTicks % 8 == 0) level.sendParticles(ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 6, 0.8, 0.4, 0.8, 0.05);
                if (--phaseTicks <= 0) enter(Phase.WALK, 70);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Phase p = phase();
        if (p == Phase.EXHAUSTED) amount *= 2.0F;
        else if (p == Phase.REST) amount *= 1.5F;
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.5F, 0.6F);
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !dead && !isRemoved();
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            ItemEntity egg = spawnAtLocation(new ItemStack(ModFlesh.FLESHLING_EGG.get()));
            if (egg != null) { egg.setUnlimitedLifetime(); egg.setGlowingTag(true); }
            spawnAtLocation(new ItemStack(ModFlesh.FLESH_ITEM.get(), 6 + random.nextInt(6)));
            for (FleshlingEntity f : level.getEntitiesOfClass(FleshlingEntity.class, new AABB(blockPosition()).inflate(32), f -> !f.isTame())) {
                level.sendParticles(GloveEffects.BLOOD_DROP, f.getX(), f.getY() + 0.5, f.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                f.discard();
            }
        }
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Nullable @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 0.6F; }
}
