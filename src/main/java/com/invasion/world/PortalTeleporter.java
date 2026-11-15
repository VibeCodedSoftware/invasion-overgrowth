package com.invasion.world;

import com.invasion.InvasionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class PortalTeleporter {

    public static void travel(ServerPlayer player, ServerLevel from) {
        MinecraftServer server = player.server;
        InvasionData data = InvasionData.get(server);
        player.setPortalCooldown(100);

        if (from.dimension() == InvasionMod.OVERGROWTH) {
            ServerLevel overworld = server.overworld();
            BlockPos portal = data.portalPos();
            BlockPos dest = portal != null ? portal.offset(0, 0, 2) : overworld.getSharedSpawnPos();
            overworld.getChunk(dest);
            if (portal == null) dest = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dest);
            player.teleportTo(overworld, dest.getX() + 1.0, dest.getY(), dest.getZ() + 0.5, 0F, 0F);
        } else {
            ServerLevel overgrowth = server.getLevel(InvasionMod.OVERGROWTH);
            if (overgrowth == null) {
                player.displayClientMessage(Component.literal("The Overgrowth dimension is missing."), true);
                return;
            }
            sendToTemple(player, overgrowth, data);
        }
        player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.25F, 1.2F);
    }

    public static void sendToTemple(ServerPlayer player, ServerLevel overgrowth, InvasionData data) {
        boolean firstVisit = data.templeCenter() == null;
        if (firstVisit) player.displayClientMessage(Component.translatable("message.invasion.temple_rising"), true);
        TempleBuilder.ensureReady(overgrowth, data);
        BlockPos a = data.templeArrival();
        player.setPortalCooldown(100);
        player.teleportTo(overgrowth, a.getX() + 1.0, a.getY(), a.getZ() + 0.5, 0F, 0F);
    }

    private PortalTeleporter() {}
}
