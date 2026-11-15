package com.invasion.entity;

import com.invasion.InvasionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ent: a tall, slow tree-giant. Peaceful until struck, then it hits back hard (and its neighbours join in).
 * It plants flowers and saplings as it wanders and attacks invaders on sight. Oak, birch and azalea kinds.
 */
public class EntEntity extends PathfinderMob {
    public enum Kind { OAK, BIRCH, AZALEA }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(EntEntity.class, EntityDataSerializers.INT);
    private int attackAnimationTick;
    private int plantCooldown = 600;

    public EntEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createEntAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    public static boolean checkEntSpawnRules(EntityType<EntEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)];
    }

    public void setKind(Kind kind) {
        entityData.set(KIND, kind.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, InvaderZombie.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        Holder<Biome> biome = level.getBiome(blockPosition());
        String path = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (path.contains("birch")) setKind(Kind.BIRCH);
        else if (path.contains("giant_oak")) setKind(Kind.OAK);
        else if (path.contains("mixed")) setKind(Kind.values()[random.nextInt(3)]);
        else if (level.getLevel().dimension() == InvasionMod.OVERGROWTH || path.contains("lush") || path.contains("azalea")) setKind(Kind.AZALEA);
        else setKind(Kind.OAK);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        attackAnimationTick = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(target);
        if (hit) target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
        playSound(SoundEvents.WOOD_BREAK, 1.0F, 0.6F);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnimationTick = 10;
        else super.handleEntityEvent(id);
    }

    public int getAttackAnimationTick() {
        return attackAnimationTick;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnimationTick > 0) attackAnimationTick--;
        if (level() instanceof ServerLevel level && getTarget() == null && --plantCooldown <= 0) {
            plantCooldown = 400 + random.nextInt(600);
            if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) plantSomething(level);
        }
    }

    private void plantSomething(ServerLevel level) {
        BlockPos p = blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos at = p.above(dy);
            if (!level.isEmptyBlock(at) || !level.getBlockState(at.below()).is(BlockTags.DIRT)) continue;
            Block[] choices = switch (kind()) {
                case OAK -> new Block[]{Blocks.POPPY, Blocks.DANDELION, Blocks.SHORT_GRASS, Blocks.OAK_SAPLING, Blocks.CORNFLOWER};
                case BIRCH -> new Block[]{Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.SHORT_GRASS, Blocks.BIRCH_SAPLING};
                case AZALEA -> new Block[]{Blocks.AZALEA, Blocks.FLOWERING_AZALEA, Blocks.MOSS_CARPET, Blocks.SHORT_GRASS};
            };
            BlockState state = choices[random.nextInt(choices.length)].defaultBlockState();
            if (state.canSurvive(level, at)) level.setBlock(at, state, 3);
            return;
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        switch (kind()) {
            case OAK -> { spawnAtLocation(new ItemStack(Items.OAK_LOG, 3 + random.nextInt(4))); spawnAtLocation(new ItemStack(Items.OAK_SAPLING, 1 + random.nextInt(2))); }
            case BIRCH -> { spawnAtLocation(new ItemStack(Items.BIRCH_LOG, 3 + random.nextInt(4))); spawnAtLocation(new ItemStack(Items.BIRCH_SAPLING, 1 + random.nextInt(2))); }
            case AZALEA -> { spawnAtLocation(new ItemStack(Items.OAK_LOG, 3 + random.nextInt(4))); spawnAtLocation(new ItemStack(Items.FLOWERING_AZALEA, 1 + random.nextInt(2))); }
        }
        if (random.nextInt(3) == 0) spawnAtLocation(new ItemStack(Items.MOSS_BLOCK, 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.06F) {
            spawnAtLocation(new ItemStack(switch (kind()) {
                case OAK -> com.invasion.ModRegistry.GIANT_OAK_SAPLING_ITEM.get();
                case BIRCH -> com.invasion.ModRegistry.GIANT_BIRCH_SAPLING_ITEM.get();
                case AZALEA -> com.invasion.ModRegistry.GIANT_AZALEA_SAPLING_ITEM.get();
            }));
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextBoolean() ? SoundEvents.WOOD_STEP : SoundEvents.AZALEA_LEAVES_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOOD_STEP, 0.6F, 0.5F);
    }

    @Override
    protected float getSoundVolume() {
        return 1.2F;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", kind().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            setKind(Kind.valueOf(tag.getString("Kind")));
        } catch (IllegalArgumentException ignored) {
            setKind(Kind.OAK);
        }
    }
}
