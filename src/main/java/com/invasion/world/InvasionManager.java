package com.invasion.world;

import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;
import com.invasion.ModRegistry;
import com.invasion.entity.InvaderZombie;
import com.invasion.entity.MeteorEntity;
import com.invasion.entity.TempleGuardian;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class InvasionManager {

    /** Runs every server tick; starts the invasion once the configured number of days has passed. */
    public static void tick(MinecraftServer server) {
        if (InvasionConfig.HORDE_PURSUES.get() && server.getTickCount() % 20 == 0) pursue(server);
        if (server.getTickCount() % 40 != 0 || !InvasionConfig.AUTO_INVASION.get()) return;
        ServerLevel overworld = server.overworld();
        InvasionData data = InvasionData.get(server);
        if (data.state() != InvasionData.State.WAITING || overworld.players().isEmpty()) return;
        if (overworld.getDayTime() >= InvasionConfig.INVASION_DAYS.get() * 24000L) {
            start(server, null);
        }
    }

    /**
     * The horde doesn't stay at the portal: every so often a small group claws its way out of the ground
     * 28-44 blocks from each survival player who is far from the portal, and hunts them.
     */
    private static void pursue(MinecraftServer server) {
        InvasionData data = InvasionData.get(server);
        if (!data.isActive()) return;
        ServerLevel level = server.overworld();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        BlockPos portal = data.portalPos();
        int interval = InvasionConfig.PURSUIT_INTERVAL.get();
        RandomSource random = level.random;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) continue;
            if (portal != null && player.blockPosition().closerThan(portal, 96)) continue; // the portal itself handles you
            if ((server.getTickCount() + player.getId() * 37) % interval >= 20) continue;
            int near = level.getEntitiesOfClass(InvaderZombie.class, player.getBoundingBox().inflate(48)).size();
            int toSpawn = Math.min(2 + random.nextInt(2), Math.max(0, InvasionConfig.MAX_HORDE.get() / 2 - near));
            int spawned = 0;
            for (int attempt = 0; attempt < 24 && spawned < toSpawn; attempt++) {
                float a = random.nextFloat() * Mth.TWO_PI;
                double d = 28 + random.nextInt(17);
                int x = Mth.floor(player.getX() + Mth.cos(a) * d), z = Mth.floor(player.getZ() + Mth.sin(a) * d);
                if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
                BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (!level.getBlockState(at.below()).isSolid() || !level.getFluidState(at).isEmpty()
                        || !level.noCollision(ModRegistry.INVADER.get().getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) continue;
                InvaderZombie zombie = random.nextInt(100) < 40 ? ModRegistry.INVADER_MINER.get().create(level) : ModRegistry.INVADER.get().create(level);
                if (zombie == null) continue;
                zombie.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0F);
                zombie.equipForInvasion(random);
                zombie.makeHunter();
                zombie.setTarget(player);
                level.addFreshEntity(zombie);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.02);
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(at.below())),
                        at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 30, 0.5, 0.2, 0.5, 0.1);
                spawned++;
            }
            if (spawned > 0) player.playNotifySound(SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
    }

    /** Launches the meteor near {@code near} (or a random Overworld player). Returns false if nobody is in the Overworld. */
    public static boolean start(MinecraftServer server, @Nullable ServerPlayer near) {
        ServerLevel overworld = server.overworld();
        ServerPlayer player = near;
        if (player == null || player.level() != overworld) {
            List<ServerPlayer> players = overworld.players();
            if (players.isEmpty()) return false;
            player = players.get(overworld.random.nextInt(players.size()));
        }
        RandomSource random = overworld.random;

        // Impact point: 45-75 blocks from the player, in a random direction.
        float angle = random.nextFloat() * Mth.TWO_PI;
        double dist = 45 + random.nextInt(30);
        int tx = Mth.floor(player.getX() + Mth.cos(angle) * dist);
        int tz = Mth.floor(player.getZ() + Mth.sin(angle) * dist);
        overworld.getChunk(tx >> 4, tz >> 4);
        BlockPos ground = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(tx, 0, tz));
        Vec3 target = Vec3.atBottomCenterOf(ground);

        // Start on the far side of the player, high up, so it streaks right overhead.
        double sx = player.getX() - Mth.cos(angle) * 35;
        double sz = player.getZ() - Mth.sin(angle) * 35;
        double sy = Math.min(target.y + 150, overworld.getMaxBuildHeight() - 10);
        MeteorEntity meteor = new MeteorEntity(overworld, new Vec3(sx, sy, sz), target);
        overworld.addFreshEntity(meteor);

        InvasionData data = InvasionData.get(server);
        data.setState(InvasionData.State.METEOR_FALLING);
        data.setBossSpawned(false);

        broadcast(server, Component.literal("A blood-red meteor is tearing across the sky!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        for (ServerPlayer p : overworld.players()) {
            p.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 0.5F);
        }
        InvasionMod.LOGGER.info("Invasion started: meteor heading for {}", ground);
        return true;
    }

    public static void onMeteorImpact(ServerLevel level, BlockPos ground) {
        BlockPos core = PortalBuilder.meteorImpact(level, ground, level.random);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 3, 3, 1, 3, 0);
        level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 16.0F, 0.5F);

        InvasionData data = InvasionData.get(level.getServer());
        data.setPortalPos(core);
        data.setState(InvasionData.State.ACTIVE);
        broadcast(level.getServer(), Component.literal("The meteor has torn open a portal at " + core.getX() + ", " + core.getY() + ", " + core.getZ()
                + ". The horde is coming. Defeat the temple guardian to stop it!").withStyle(ChatFormatting.RED));
    }

    public static void onBossDefeated(MinecraftServer server) {
        InvasionData data = InvasionData.get(server);
        data.setState(InvasionData.State.DEFEATED);
        crumbleInvaders(server);
        broadcast(server, Component.literal("The temple guardian has fallen. The invasion is over!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }

    /** /hw invasion stop: ends the horde and removes the boss without a reward. */
    public static void stop(MinecraftServer server) {
        InvasionData data = InvasionData.get(server);
        data.setState(InvasionData.State.DEFEATED);
        crumbleInvaders(server);
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> bosses = new ArrayList<>();
            for (Entity e : level.getAllEntities()) if (e instanceof TempleGuardian || e instanceof MeteorEntity) bosses.add(e);
            bosses.forEach(Entity::discard);
        }
        data.setBossSpawned(false);
    }

    private static void crumbleInvaders(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            List<InvaderZombie> invaders = new ArrayList<>();
            for (Entity e : level.getAllEntities()) if (e instanceof InvaderZombie z && z.isAlive()) invaders.add(z);
            for (InvaderZombie z : invaders) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, z.getX(), z.getY() + 1, z.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                z.discard();
            }
        }
    }

    public static Component status(MinecraftServer server) {
        InvasionData data = InvasionData.get(server);
        long day = server.overworld().getDayTime() / 24000L;
        String text = switch (data.state()) {
            case WAITING -> "No invasion yet. Day " + day + " of " + InvasionConfig.INVASION_DAYS.get()
                    + (InvasionConfig.AUTO_INVASION.get() ? "" : " (automatic invasion disabled)");
            case METEOR_FALLING -> "A meteor is falling!";
            case ACTIVE -> "Invasion ACTIVE. Portal at " + data.portalPos().toShortString();
            case DEFEATED -> "The invasion has been defeated.";
        };
        return Component.literal("[Invasion] " + text);
    }

    private static void broadcast(MinecraftServer server, Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private InvasionManager() {}
}
