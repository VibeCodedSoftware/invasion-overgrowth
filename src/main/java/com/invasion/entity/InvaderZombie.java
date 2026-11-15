package com.invasion.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.particles.ParticleTypes;
import java.util.EnumSet;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Sword-wielding zombie from the portal. Never burns, never gives up, and slowly hacks through walls to reach you. */
public class InvaderZombie extends Zombie {
    /** Came through the invasion portal: hunts the nearest Overworld player wherever they are, never despawns. */
    private boolean hunter;

    public InvaderZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createInvaderAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    public boolean isHunter() {
        return hunter;
    }

    public void makeHunter() {
        hunter = true;
        setPersistenceRequired();
        updateSenses();
    }

    /** Hunters sense a long way (they are given their target directly); Overgrowth locals only notice you close by. */
    private void updateSenses() {
        var range = getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) range.setBaseValue(hunter ? 96.0 : 20.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hunter", hunter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.getBoolean("Hunter");
        updateSenses();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (tickCount % 40 == 0 && level() instanceof ServerLevel server) {
            if (tickCount == 40) updateSenses();
            // the invasion is over: hunters that were unloaded when the guardian fell crumble now
            if (hunter && com.invasion.world.InvasionData.get(server.getServer()).state() == com.invasion.world.InvasionData.State.DEFEATED) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                discard();
            }
        }
    }

    /** How fast this invader breaks blocks (1 = a miner with a pickaxe). */
    public float digSpeed() {
        return 0.5F;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new InvaderAssaultGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new HuntPlayerGoal(this));
        // Overgrowth locals: only players they can see, close by (follow range 20).
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, EntEntity.class, true));
    }

    public void equipForInvasion(RandomSource r) {
        int roll = r.nextInt(100);
        Item sword = roll < 30 ? Items.WOODEN_SWORD : roll < 65 ? Items.STONE_SWORD : roll < 88 ? Items.IRON_SWORD
                : roll < 95 ? Items.GOLDEN_SWORD : Items.DIAMOND_SWORD;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(sword));
        setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        equipForInvasion(random);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {}

    /** Locks a hunter onto the nearest survival player in its dimension, at any distance, and keeps it there. */
    static class HuntPlayerGoal extends Goal {
        private final InvaderZombie mob;
        @org.jetbrains.annotations.Nullable private Player prey;

        HuntPlayerGoal(InvaderZombie mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @org.jetbrains.annotations.Nullable
        private Player nearest() {
            Player best = null;
            double bestD = Double.MAX_VALUE;
            for (Player p : mob.level().players()) {
                if (!valid(p)) continue;
                double d = p.distanceToSqr(mob);
                if (d < bestD) { bestD = d; best = p; }
            }
            return best;
        }

        private static boolean valid(Player p) {
            return p.isAlive() && !p.isCreative() && !p.isSpectator();
        }

        @Override
        public boolean canUse() {
            if (!mob.hunter || mob.tickCount % 10 != 0) return false;
            prey = nearest();
            return prey != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (prey == null || !valid(prey) || prey.level() != mob.level()) return false;
            if (mob.tickCount % 40 == 0) {
                Player closer = nearest();
                if (closer != null && closer != prey) { prey = closer; mob.setTarget(prey); }
            }
            if (mob.getTarget() != prey) mob.setTarget(prey);
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(prey);
        }

        @Override
        public void stop() {
            if (mob.getTarget() == prey) mob.setTarget(null);
            prey = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }
    }
}
