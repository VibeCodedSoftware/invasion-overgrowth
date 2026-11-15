package com.invasion.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** World-wide invasion state, stored with the Overworld. */
public class InvasionData extends SavedData {
    public enum State { WAITING, METEOR_FALLING, ACTIVE, DEFEATED }

    private State state = State.WAITING;
    @Nullable private BlockPos portalPos;
    @Nullable private BlockPos templeCenter;
    @Nullable private BlockPos templeArrival;
    private boolean bossSpawned;

    public static InvasionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InvasionData::new, InvasionData::load, null), "invasion_state");
    }

    private static InvasionData load(CompoundTag tag, HolderLookup.Provider registries) {
        InvasionData d = new InvasionData();
        try {
            d.state = State.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException ignored) {
            d.state = State.WAITING;
        }
        d.portalPos = NbtUtils.readBlockPos(tag, "Portal").orElse(null);
        d.templeCenter = NbtUtils.readBlockPos(tag, "Temple").orElse(null);
        d.templeArrival = NbtUtils.readBlockPos(tag, "Arrival").orElse(null);
        d.bossSpawned = tag.getBoolean("BossSpawned");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("State", state.name());
        if (portalPos != null) tag.put("Portal", NbtUtils.writeBlockPos(portalPos));
        if (templeCenter != null) tag.put("Temple", NbtUtils.writeBlockPos(templeCenter));
        if (templeArrival != null) tag.put("Arrival", NbtUtils.writeBlockPos(templeArrival));
        tag.putBoolean("BossSpawned", bossSpawned);
        return tag;
    }

    public State state() { return state; }
    public boolean isActive() { return state == State.ACTIVE; }
    @Nullable public BlockPos portalPos() { return portalPos; }
    @Nullable public BlockPos templeCenter() { return templeCenter; }
    @Nullable public BlockPos templeArrival() { return templeArrival; }
    public boolean bossSpawned() { return bossSpawned; }

    public void setState(State s) { state = s; setDirty(); }
    public void setPortalPos(BlockPos p) { portalPos = p; setDirty(); }
    public void setTemple(BlockPos center, BlockPos arrival) { templeCenter = center; templeArrival = arrival; setDirty(); }
    public void setBossSpawned(boolean b) { bossSpawned = b; setDirty(); }
}
