package com.invasion.entity;

import com.invasion.ModRegistry;
import com.invasion.item.GloveCharge;
import com.invasion.item.GloveCharge.Element;
import com.invasion.world.InvasionManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * The temple boss. It fights in a readable, repeating pattern with telegraphed wind-ups and openings:
 * FIRE stream -> rest -> REDSTONE volley -> rest -> LEAP slam -> EXHAUSTED (kneels, glows, takes double damage).
 * Below half health it adds a WATER dash, calls for help once, and rests a little less.
 */
public class TempleGuardian extends Zombie {
    private enum Phase { WALK, FIRE_WINDUP, FIRE, REST, BARRAGE_WINDUP, BARRAGE, LEAP_WINDUP, LEAPING, EXHAUSTED, DASH_WINDUP, DASH }

    private static final Phase[] CYCLE = {Phase.FIRE_WINDUP, Phase.REST, Phase.BARRAGE_WINDUP, Phase.REST, Phase.LEAP_WINDUP, Phase.EXHAUSTED};
    private static final Phase[] CYCLE_ENRAGED = {Phase.FIRE_WINDUP, Phase.REST, Phase.BARRAGE_WINDUP, Phase.DASH_WINDUP, Phase.REST, Phase.LEAP_WINDUP, Phase.EXHAUSTED};

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.invasion.temple_guardian"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private Phase phase = Phase.WALK;
    private int phaseTicks = 80;
    private int cycleIndex;
    private boolean summonedHelp;
    private boolean wasAirborne;
    private final Predicate<LivingEntity> canHit = e -> !(e instanceof InvaderZombie) && !(e instanceof TempleGuardian);

    public TempleGuardian(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 250;
        setCanPickUpLoot(false);
        setItemSlot(EquipmentSlot.MAINHAND, gauntlet());
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static ItemStack gauntlet() {
        ItemStack stack = new ItemStack(ModRegistry.PARTICLE_GLOVE.get());
        stack.set(ModRegistry.GLOVE_CHARGE.get(), new GloveCharge(GloveCharge.MAX, GloveCharge.MAX, GloveCharge.MAX, 0));
        return stack;
    }

    public static AttributeSupplier.Builder createGuardianAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.SCALE, 1.75)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() { return phase == Phase.WALK && super.canUse(); }
            @Override public boolean canContinueToUse() { return phase == Phase.WALK && super.canContinueToUse(); }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private boolean enraged() {
        return getHealth() < getMaxHealth() / 2;
    }

    private void enter(Phase p, int ticks) {
        phase = p;
        phaseTicks = ticks;
        setGlowingTag(p == Phase.EXHAUSTED);
    }

    private void nextInCycle() {
        Phase[] cycle = enraged() ? CYCLE_ENRAGED : CYCLE;
        Phase next = cycle[cycleIndex % cycle.length];
        cycleIndex++;
        int rest = enraged() ? 30 : 45;
        switch (next) {
            case FIRE_WINDUP, BARRAGE_WINDUP, DASH_WINDUP -> enter(next, 20);
            case LEAP_WINDUP -> enter(next, 16);
            case REST -> enter(next, rest);
            case EXHAUSTED -> enter(next, enraged() ? 60 : 80);
            default -> enter(Phase.WALK, 60);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (phase != Phase.WALK) enter(Phase.WALK, 60);
            return;
        }

        if (!summonedHelp && enraged()) {
            summonedHelp = true;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
            RandomSource r = getRandom();
            for (int i = 0; i < 3; i++) {
                InvaderZombie z = (i == 0 ? ModRegistry.INVADER_MINER.get() : ModRegistry.INVADER.get()).create(level);
                if (z == null) continue;
                z.moveTo(getX() + r.nextInt(5) - 2, getY(), getZ() + r.nextInt(5) - 2, r.nextFloat() * 360, 0);
                z.equipForInvasion(r);
                z.setTarget(target);
                level.addFreshEntity(z);
            }
        }

        Vec3 dir = target.getEyePosition().subtract(getEyePosition()).normalize();
        Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize();
        boolean stationary = phase != Phase.WALK && phase != Phase.LEAPING && phase != Phase.DASH;
        if (stationary) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        if (phase != Phase.EXHAUSTED) getLookControl().setLookAt(target, 40, 40);
        Vec3 hand = getEyePosition().add(flat.scale(1.2)).add(0, -0.6, 0);

        switch (phase) {
            case WALK -> { if (--phaseTicks <= 0) nextInCycle(); }
            case FIRE_WINDUP -> {
                level.sendParticles(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 3, 0.3, 0.3, 0.3, 0.01);
                if (phaseTicks == 20) level.playSound(null, getX(), getY(), getZ(), ModRegistry.GLOVE_CHARGE_SOUND.get(), SoundSource.HOSTILE, 1.5F, 0.6F);
                if (--phaseTicks <= 0) {
                    enter(Phase.FIRE, 40);
                    level.playSound(null, getX(), getY(), getZ(), ModRegistry.GLOVE_FIRE.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
                }
            }
            case FIRE -> {
                GloveEffects.fireStream(level, this, dir, 14.0, phaseTicks % 10 == 0 ? 3.0F : 0.0F, canHit);
                if (--phaseTicks <= 0) nextInCycle();
            }
            case BARRAGE_WINDUP -> {
                level.sendParticles(GloveEffects.RED_DUST, hand.x, hand.y, hand.z, 4, 0.3, 0.3, 0.3, 0);
                if (phaseTicks == 20) level.playSound(null, getX(), getY(), getZ(), ModRegistry.GLOVE_CHARGE_SOUND.get(), SoundSource.HOSTILE, 1.5F, 1.2F);
                if (--phaseTicks <= 0) enter(Phase.BARRAGE, 45);
            }
            case BARRAGE -> {
                if (phaseTicks % 15 == 0) GloveEffects.redstoneBolt(level, this, dir, 28.0, 5.0F, 1.1, canHit);
                if (--phaseTicks <= 0) nextInCycle();
            }
            case DASH_WINDUP -> {
                level.sendParticles(ParticleTypes.SPLASH, hand.x, hand.y, hand.z, 6, 0.3, 0.3, 0.3, 0.1);
                if (--phaseTicks <= 0) {
                    enter(Phase.DASH, 16);
                    level.playSound(null, getX(), getY(), getZ(), ModRegistry.GLOVE_WATER.get(), SoundSource.HOSTILE, 2.0F, 0.7F);
                }
            }
            case DASH -> {
                GloveEffects.waterJet(level, this, flat.scale(-1), 3.0, 0.0F, 0.0, canHit);
                setDeltaMovement(flat.scale(0.8).add(0, getDeltaMovement().y, 0));
                hurtMarked = true;
                if (distanceTo(target) < 2.5) {
                    doHurtTarget(target);
                    nextInCycle();
                } else if (--phaseTicks <= 0) nextInCycle();
            }
            case LEAP_WINDUP -> {
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 2, 0.6, 0.0, 0.6, 0.02);
                if (phaseTicks == 16) level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.3F, 1.3F);
                if (--phaseTicks <= 0) {
                    double dist = Math.min(distanceTo(target), 14);
                    setDeltaMovement(flat.x * dist * 0.11, 0.85, flat.z * dist * 0.11);
                    hurtMarked = true;
                    wasAirborne = false;
                    enter(Phase.LEAPING, 60);
                }
            }
            case LEAPING -> {
                if (!onGround()) wasAirborne = true;
                if ((wasAirborne && onGround()) || --phaseTicks <= 0) {
                    GloveEffects.shockwave(level, this, Element.byIndex(random.nextInt(3)), 6.0, 6.0F, canHit);
                    nextInCycle();
                }
            }
            case REST -> {
                if (phaseTicks % 10 == 0) level.sendParticles(ParticleTypes.POOF, getX(), getEyeY(), getZ(), 2, 0.2, 0.1, 0.2, 0.01);
                if (--phaseTicks <= 0) enter(Phase.WALK, enraged() ? 40 : 60);
            }
            case EXHAUSTED -> {
                if (phaseTicks % 8 == 0) level.sendParticles(ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 4, 0.4, 0.3, 0.4, 0.05);
                if (--phaseTicks <= 0) enter(Phase.WALK, enraged() ? 40 : 60);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (phase == Phase.EXHAUSTED) amount *= 2.0F;
        else if (phase == Phase.REST) amount *= 1.5F;
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return phase != Phase.EXHAUSTED && super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        boolean wasAlive = !dead && !isRemoved();
        super.die(source);
        if (wasAlive && level() instanceof ServerLevel level) {
            ItemEntity drop = spawnAtLocation(new ItemStack(ModRegistry.PARTICLE_GLOVE.get()));
            if (drop != null) {
                drop.setUnlimitedLifetime();
                drop.setGlowingTag(true);
            }
            InvasionManager.onBossDefeated(level.getServer());
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, gauntlet());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {}

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
