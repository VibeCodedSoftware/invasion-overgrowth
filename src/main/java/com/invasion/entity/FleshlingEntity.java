package com.invasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Fleshling. Wild ones are the Flesh Thing's toothy minions. One hatched from a Fleshling Egg is a loyal pet:
 * it follows its owner, sits on command, fights whatever hurts its owner, and can't die - when it would, it
 * collapses into a dormant lump and regrows over ten seconds.
 */
public class FleshlingEntity extends Monster implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(FleshlingEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(FleshlingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DORMANT = SynchedEntityData.defineId(FleshlingEntity.class, EntityDataSerializers.INT);

    public FleshlingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createFleshlingAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(SITTING, false);
        builder.define(DORMANT, 0);
    }

    // ---- ownership ----
    public boolean isTame() { return entityData.get(OWNER).isPresent(); }
    @Nullable @Override public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }
    public boolean isSitting() { return entityData.get(SITTING); }
    public int dormantTicks() { return entityData.get(DORMANT); }

    public void tameTo(Player player) {
        entityData.set(OWNER, Optional.of(player.getUUID()));
        setPersistenceRequired();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0);
        setHealth(getMaxHealth());
        setTarget(null);
    }

    private boolean isOwner(Entity e) {
        return e != null && isTame() && e.getUUID().equals(getOwnerUUID());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new StayGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(4, new FollowOwner());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendOwner());
        targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return super.canUse() && !isOwner(getLastHurtByMob()); }
        });
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canUse() { return !isTame() && super.canUse(); }
        });
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true) {
            @Override public boolean canUse() { return !isTame() && super.canUse(); }
        });
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isOwner(target)) return false;
        if (isTame() && target instanceof FleshlingEntity other && other.isTame()) return false;
        if (isTame() && target instanceof Player) return false;
        return super.canAttack(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        int dormant = dormantTicks();
        if (dormant > 0) {
            entityData.set(DORMANT, dormant - 1);
            getNavigation().stop();
            setTarget(null);
            heal(getMaxHealth() / 200.0F);
            if (dormant % 10 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(GloveEffects.BLOOD_DROP, getX(), getY() + 0.4, getZ(), 4, 0.3, 0.2, 0.3, 0);
            }
            if (dormant == 1) playSound(SoundEvents.SLIME_JUMP, 1.0F, 0.6F);
        }
        if (isTame() && getY() < level().getMinBuildHeight() - 5) {
            Player owner = level().getPlayerByUUID(getOwnerUUID());
            if (owner != null) { teleportTo(owner.getX(), owner.getY(), owner.getZ()); fallDistance = 0; }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isTame()) {
            if (isOwner(source.getEntity())) return false;
            if (dormantTicks() > 0) return false;
            if (amount >= getHealth()) {
                // a pet Fleshling can't die: it collapses and regrows instead
                amount = Math.max(0, getHealth() - 1);
                entityData.set(DORMANT, 200);
                Player owner = level().getPlayerByUUID(getOwnerUUID());
                if (owner != null) owner.displayClientMessage(Component.translatable("message.invasion.fleshling_dormant"), true);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwner(player)) return super.mobInteract(player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN) || stack.is(Items.MUTTON)) {
            if (getHealth() < getMaxHealth()) {
                if (!level().isClientSide) {
                    stack.consume(1, player);
                    heal(8.0F);
                    playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.7F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (stack.isEmpty()) {
            if (!level().isClientSide) {
                entityData.set(SITTING, !isSitting());
                getNavigation().stop();
                setTarget(null);
                player.displayClientMessage(Component.translatable(isSitting() ? "message.invasion.fleshling_sit" : "message.invasion.fleshling_follow"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.6F, 1.4F);
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTame() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return !isTame();
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return !isTame() && super.isPreventingPlayerRest(player);
    }

    @Nullable @Override protected SoundEvent getAmbientSound() { return SoundEvents.SLIME_SQUISH; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH; }
    @Override public float getVoicePitch() { return 0.7F + random.nextFloat() * 0.2F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Sitting", isSitting());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(SITTING, tag.getBoolean("Sitting"));
    }

    // ---- pet goals ----
    private class StayGoal extends Goal {
        StayGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return (isTame() && isSitting()) || dormantTicks() > 0; }
        @Override public void start() { getNavigation().stop(); }
    }

    private class FollowOwner extends Goal {
        FollowOwner() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Nullable private Player owner() { return isTame() ? level().getPlayerByUUID(getOwnerUUID()) : null; }

        @Override public boolean canUse() {
            Player o = owner();
            return o != null && !isSitting() && getTarget() == null && distanceToSqr(o) > 36;
        }

        @Override public boolean canContinueToUse() {
            Player o = owner();
            return o != null && !isSitting() && getTarget() == null && distanceToSqr(o) > 9;
        }

        @Override public void tick() {
            Player o = owner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10, getMaxHeadXRot());
            if (distanceToSqr(o) > 24 * 24) {
                teleportTo(o.getX() + random.nextInt(3) - 1, o.getY(), o.getZ() + random.nextInt(3) - 1);
                getNavigation().stop();
            } else if (tickCount % 10 == 0) {
                getNavigation().moveTo(o, 1.2);
            }
        }

        @Override public void stop() { getNavigation().stop(); }
    }

    /** Attacks whatever hurt the owner, or whatever the owner just attacked. */
    private class DefendOwner extends TargetGoal {
        @Nullable private LivingEntity pending;

        DefendOwner() {
            super(FleshlingEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override public boolean canUse() {
            if (!isTame() || isSitting() || dormantTicks() > 0) return false;
            Player o = level().getPlayerByUUID(getOwnerUUID());
            if (o == null) return false;
            LivingEntity t = o.getLastHurtByMob();
            if (t == null || o.tickCount - o.getLastHurtByMobTimestamp() > 100) t = o.getLastHurtMob();
            if (t == null || isOwner(t) || !canAttack(t, TargetingConditions.DEFAULT)) return false;
            pending = t;
            return true;
        }

        @Override public void start() {
            mob.setTarget(pending);
            super.start();
        }
    }
}
