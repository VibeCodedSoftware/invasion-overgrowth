package com.invasion.world;

import com.invasion.InvasionMod;
import com.invasion.ModFluids;
import com.invasion.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Overgrown Dungeon: a huge mossy monster hall underground (invader spawners, pillars, a blood-geyser altar)
 * with four cobwebbed corridors leading to side rooms holding loot chests, miner spawners and a blood pool.
 * Fits inside the 3x3 chunk area a feature may write to.
 */
public class OvergrownDungeonFeature extends Feature<NoneFeatureConfiguration> {
    static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, InvasionMod.id("chests/overgrown_dungeon"));

    public OvergrownDungeonFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        BlockPos c = new BlockPos((o.getX() & ~15) + 8, o.getY(), (o.getZ() & ~15) + 8);
        if (c.getY() < level.getMinBuildHeight() + 6) return false;
        // stay fully buried: skip spots too close to the surface (valleys, oceans, cliffs)
        for (int[] s : new int[][]{{0, 0}, {-22, 0}, {22, 0}, {0, -22}, {0, 22}, {-16, -16}, {16, 16}, {-16, 16}, {16, -16}}) {
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, c.getX() + s[0], c.getZ() + s[1]);
            if (surface < c.getY() + 16) return false;
        }

        // Main hall: 13x7x13 interior.
        room(level, r, c, 6, 7, 6);
        for (int[] p : new int[][]{{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}) {
            for (int y = 0; y < 7; y++) set(level, c.offset(p[0], y, p[1]), y == 3 ? Blocks.VERDANT_FROGLIGHT.defaultBlockState() : brick(r));
        }
        // Blood geyser altar in the middle.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(level, c.offset(dx, 0, dz), ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState());
        set(level, c.above(), ModRegistry.BLOOD_GEYSER.get().defaultBlockState());
        spawner(level, r, c.offset(0, 0, -4), ModRegistry.INVADER.get());
        spawner(level, r, c.offset(0, 0, 4), ModRegistry.INVADER.get());
        set(level, c.offset(-5, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(level, c.offset(5, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // Corridors and side rooms.
        int roomIndex = 0;
        int bloodRoom = r.nextInt(4);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int i = 7; i <= 14; i++) {
                BlockPos mid = c.relative(d, i);
                Direction side = d.getClockWise();
                for (int w = -2; w <= 2; w++) {
                    for (int y = -1; y <= 4; y++) {
                        BlockPos p = mid.relative(side, w).above(y);
                        boolean wall = Math.abs(w) == 2 || y == -1 || y == 4;
                        if (wall) set(level, p, y == -1 ? floor(r) : brick(r));
                        else set(level, p, (y == 3 && r.nextInt(9) == 0) ? Blocks.COBWEB.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
                    }
                }
                if (r.nextInt(5) == 0) set(level, mid.relative(side, r.nextBoolean() ? 1 : -1), Blocks.MOSS_CARPET.defaultBlockState());
            }
            BlockPos rc = c.relative(d, 18);
            room(level, r, rc, 3, 5, 3);
            // doorway back toward the hall
            for (int y = 0; y < 3; y++) for (int w = -1; w <= 1; w++) set(level, rc.relative(d.getOpposite(), 4).relative(d.getClockWise(), w).above(y), Blocks.CAVE_AIR.defaultBlockState());
            BlockPos chest = rc.relative(d, 2);
            set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, d.getOpposite()));
            RandomizableContainer.setBlockEntityLootTable(level, r, chest, LOOT);
            if (roomIndex == bloodRoom) {
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(level, rc.offset(dx, -1, dz), ModFluids.BLOOD_BLOCK.get().defaultBlockState());
            } else if (r.nextBoolean()) {
                spawner(level, r, rc, ModRegistry.INVADER_MINER.get());
            }
            set(level, rc.above(4), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            roomIndex++;
        }
        return true;
    }

    /** Hollow room with mossy walls; interior half-size (hx, hz) and height h. */
    private void room(WorldGenLevel level, RandomSource r, BlockPos c, int hx, int h, int hz) {
        for (int dx = -hx - 1; dx <= hx + 1; dx++) {
            for (int dz = -hz - 1; dz <= hz + 1; dz++) {
                for (int y = -1; y <= h; y++) {
                    BlockPos p = c.offset(dx, y, dz);
                    boolean wall = Math.abs(dx) == hx + 1 || Math.abs(dz) == hz + 1 || y == -1 || y == h;
                    if (wall) set(level, p, y == -1 ? floor(r) : brick(r));
                    else set(level, p, Blocks.CAVE_AIR.defaultBlockState());
                }
                if (Math.abs(dx) <= hx && Math.abs(dz) <= hz && r.nextInt(7) == 0) set(level, c.offset(dx, 0, dz), Blocks.MOSS_CARPET.defaultBlockState());
                if (Math.abs(dx) <= hx && Math.abs(dz) <= hz && r.nextInt(14) == 0) set(level, c.offset(dx, h - 1, dz), Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private static BlockState brick(RandomSource r) {
        int roll = r.nextInt(10);
        if (roll < 4) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (roll < 7) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (roll < 9) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }

    private static BlockState floor(RandomSource r) {
        int roll = r.nextInt(10);
        if (roll < 5) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (roll < 8) return Blocks.MOSS_BLOCK.defaultBlockState();
        return ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState();
    }

    private static void spawner(WorldGenLevel level, RandomSource r, BlockPos pos, EntityType<?> type) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpawnerBlockEntity spawner) spawner.setEntityId(type, r);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).getDestroySpeed(level, pos) < 0) return; // never replace bedrock
        level.setBlock(pos, state, 2);
    }
}
