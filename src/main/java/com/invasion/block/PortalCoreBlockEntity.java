package com.invasion.block;

import com.invasion.InvasionConfig;
import com.invasion.ModRegistry;
import com.invasion.entity.InvaderZombie;
import com.invasion.world.InvasionData;
import com.invasion.world.MossSpreader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Lives in the bottom-left block of a portal. Spreads moss around the frame and,
 * for the Overworld invasion portal, pours out the never-ending zombie horde.
 */
public class PortalCoreBlockEntity extends BlockEntity {
    private int age;
    private int spawnTimer = 60;

    public PortalCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PORTAL_CORE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalCoreBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.tick(serverLevel, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        RandomSource random = level.random;

        int maxRadius = InvasionConfig.MOSS_RADIUS.get();
        if (maxRadius > 0 && age % 3 == 0) {
            int radius = Math.min(maxRadius, 3 + age / 120);
            MossSpreader.spread(level, pos, radius, random);
        }
        if (age % 100 == 0) setChanged();

        if (level.dimension() != Level.OVERWORLD) return;
        InvasionData data = InvasionData.get(level.getServer());
        if (!data.isActive() || !pos.equals(data.portalPos())) return;

        if (--spawnTimer > 0) return;
        spawnTimer = InvasionConfig.SPAWN_INTERVAL.get() + random.nextInt(60);
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 96)) return;

        int alive = level.getEntitiesOfClass(InvaderZombie.class, new AABB(pos).inflate(64)).size();
        int toSpawn = Math.min(1 + random.nextInt(3), InvasionConfig.MAX_HORDE.get() - alive);
        boolean alongX = state.getValue(InvasionPortalBlock.AXIS) == net.minecraft.core.Direction.Axis.X;
        for (int i = 0; i < toSpawn; i++) {
            InvaderZombie zombie = random.nextInt(100) < 35 ? ModRegistry.INVADER_MINER.get().create(level) : ModRegistry.INVADER.get().create(level);
            if (zombie == null) continue;
            double x = pos.getX() + (alongX ? 0.5 + random.nextInt(2) : 0.5);
            double z = pos.getZ() + (alongX ? 0.5 : 0.5 + random.nextInt(2));
            zombie.moveTo(x, pos.getY(), z, random.nextFloat() * 360F, 0F);
            zombie.equipForInvasion(random);
            zombie.makeHunter();
            double push = random.nextBoolean() ? 0.35 : -0.35;
            zombie.setDeltaMovement(alongX ? 0 : push, 0.25, alongX ? push : 0);
            level.addFreshEntity(zombie);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 1, z, 15, 0.4, 0.8, 0.4, 0.02);
        }
        if (toSpawn > 0) {
            level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.3F, 1.6F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Age", age);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        age = tag.getInt("Age");
    }
}
