package com.invasion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Dev-only checks, run with -Dinvasion.selftest=true on a dedicated server; never active in normal play. */
final class SelfTest {
    private static final String[] BIOMES = {"overgrown_wilds", "giant_oak_woods", "giant_birch_woods", "giant_mixed_woods",
            "giant_azalea_woods", "the_flesh", "dark_flesh", "blood_grove", "bloodstone_wastes"};

    static void init() {
        if (!Boolean.getBoolean("invasion.selftest")) return;
        if ("hunt".equals(System.getProperty("invasion.selftest.mode"))) { huntTest(); return; }
        if ("climate".equals(System.getProperty("invasion.selftest.mode"))) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                ServerLevel og = e.getServer().getLevel(InvasionMod.OVERGROWTH);
                var sampler = og.getChunkSource().randomState().sampler();
                int n = 0; int[][] grid = new int[20][20];
                StringBuilder raw = new StringBuilder();
                java.util.List<float[]> pts = new java.util.ArrayList<>();
                for (int gx = -60; gx < 60; gx++) for (int gz = -60; gz < 60; gz++) {
                    var p = sampler.sample(gx * 32, 16, gz * 32);
                    float t = net.minecraft.world.level.biome.Climate.unquantizeCoord(p.temperature()), h = net.minecraft.world.level.biome.Climate.unquantizeCoord(p.humidity()),
                          w = net.minecraft.world.level.biome.Climate.unquantizeCoord(p.weirdness()), d = net.minecraft.world.level.biome.Climate.unquantizeCoord(p.depth());
                    pts.add(new float[]{t, h, w, d});
                }
                for (int i = 0; i < 4; i++) {
                    final int k = i;
                    float[] v = new float[pts.size()]; for (int j = 0; j < v.length; j++) v[j] = pts.get(j)[k];
                    java.util.Arrays.sort(v);
                    StringBuilder q = new StringBuilder();
                    for (int dec = 0; dec <= 20; dec++) q.append(String.format("%.2f ", v[Math.min(v.length - 1, dec * v.length / 20)]));
                    InvasionMod.LOGGER.info("SELFTEST climate {} 5%-quantiles: {}", new String[]{"temp", "humid", "weird", "depth"}[k], q);
                }
                e.getServer().halt(false);
            });
            return;
        }
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            try {
                run(e);
            } catch (Throwable t) {
                InvasionMod.LOGGER.error("SELFTEST crashed", t);
            }
            e.getServer().halt(false);
        });
    }

    private static void run(ServerStartedEvent e) {
        ServerLevel level = e.getServer().overworld();
        BlockPos pos = new BlockPos(0, 200, 0);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, ModFluids.BLOOD_BLOCK.get().defaultBlockState(), 3);
        FakePlayer p = FakePlayerFactory.getMinecraft(level);
        p.moveTo(0.5, 201.2, 0.5, 0F, 90F);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        ItemStack out = Items.BUCKET.use(level, p, InteractionHand.MAIN_HAND).getObject();
        InvasionMod.LOGGER.info("SELFTEST bucket pickup -> {}", out);

        for (String t : new String[]{"overgrown_dungeon", "ent_village"}) {
            LootTable table = e.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, InvasionMod.id("chests/" + t)));
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            InvasionMod.LOGGER.info("SELFTEST loot {} (empty table={}) -> {}", t, table == LootTable.EMPTY, table.getRandomItems(params));
        }

        ServerLevel og = e.getServer().getLevel(InvasionMod.OVERGROWTH);
        if (og == null) { InvasionMod.LOGGER.error("SELFTEST no Overgrowth"); return; }
        var gen = og.getChunkSource().getGenerator();
        var sampler = og.getChunkSource().randomState().sampler();
        java.util.Map<String, Integer> cover = new java.util.TreeMap<>();
        int samples = 0;
        for (int gx = -50; gx < 50; gx++) for (int gz = -50; gz < 50; gz++) {
            var h = gen.getBiomeSource().getNoiseBiome(gx * 32, 16, gz * 32, sampler);
            cover.merge(h.unwrapKey().map(k -> k.location().getPath()).orElse("?"), 1, Integer::sum);
            samples++;
        }
        StringBuilder cs = new StringBuilder();
        for (var en : cover.entrySet()) cs.append(String.format("%s=%.1f%% ", en.getKey(), en.getValue() * 100.0 / samples));
        InvasionMod.LOGGER.info("SELFTEST coverage (12800x12800 blocks): {}", cs);
        int dungeonSpawners = 0, chunks = 0;
        for (String b : BIOMES) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, InvasionMod.id(b));
            var found = og.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 80, 0), 6400, 32, 64);
            if (found == null) { InvasionMod.LOGGER.warn("SELFTEST biome {} NOT FOUND within 6400", b); continue; }
            BlockPos at = found.getFirst();
            long t0 = System.currentTimeMillis();
            int cx = at.getX() >> 4, cz = at.getZ() >> 4;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                var ch = og.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
                chunks++;
                if (ch instanceof net.minecraft.world.level.chunk.LevelChunk lc)
                    for (var be : lc.getBlockEntities().values())
                        if (be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity
                                && be.saveWithoutMetadata(og.registryAccess()).toString().contains("invasion:invader")) dungeonSpawners++;
            }
            int top = og.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ());
            InvasionMod.LOGGER.info("SELFTEST biome {} at {} ({} blocks away), 9x9 chunks in {} ms, surface {} = {}", b, at,
                    (int) Math.sqrt(at.distSqr(BlockPos.ZERO)), System.currentTimeMillis() - t0, top,
                    og.getBlockState(new BlockPos(at.getX(), top - 1, at.getZ())));
        }
        InvasionMod.LOGGER.info("SELFTEST dungeons: {} invader spawners in {} chunks", dungeonSpawners, chunks);
    }

    private static final java.util.List<com.invasion.entity.InvaderZombie> HUNTERS = new java.util.ArrayList<>();
    private static net.minecraft.world.entity.npc.Villager prey;
    private static int ticks;

    /** Hunters spawned ~80 blocks from a target must close in on it. */
    private static void huntTest() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            ServerLevel level = e.getServer().overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            for (int cx = (spawn.getX() >> 4) - 2; cx <= (spawn.getX() >> 4) + 8; cx++)
                for (int cz = (spawn.getZ() >> 4) - 2; cz <= (spawn.getZ() >> 4) + 2; cz++) level.setChunkForced(cx, cz, true);
            level.getChunk((spawn.getX() + 80) >> 4, spawn.getZ() >> 4);
            BlockPos preyAt0 = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.offset(80, 0, 0));
            BlockPos preyAt = preyAt0;
            if ("tree".equals(System.getProperty("invasion.selftest.case"))) {
                for (int y = 0; y < 7; y++) level.setBlock(preyAt.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 7; dy <= 9; dy++)
                    if ((dx != 0 || dz != 0 || dy == 9) && level.getBlockState(preyAt.offset(dx, dy, dz)).isAir())
                        level.setBlock(preyAt.offset(dx, dy, dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
                preyAt = preyAt.above(7);
            }
            prey = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
            prey.moveTo(preyAt.getX() + 0.5, preyAt.getY(), preyAt.getZ() + 0.5);
            prey.setNoAi(true);
            prey.setInvulnerable(true);
            level.addFreshEntity(prey);
            for (int i = 0; i < 4; i++) {
                BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.offset(0, 0, i * 2 - 3));
                var z = (i % 2 == 0 ? ModRegistry.INVADER : ModRegistry.INVADER_MINER).get().create(level);
                z.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                z.equipForInvasion(level.random);
                z.makeHunter();
                z.setTarget(prey);
                level.addFreshEntity(z);
                HUNTERS.add(z);
            }
            InvasionMod.LOGGER.info("SELFTEST hunt: prey at {}, hunters at {}", preyAt, spawn);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (prey == null) return;
            ticks++;
            for (var z : HUNTERS) if (z.isAlive() && z.getTarget() != prey) z.setTarget(prey);
            if (ticks % 200 == 0) {
                StringBuilder sb = new StringBuilder();
                for (var z : HUNTERS) sb.append(String.format("%s%.0f ", z.isAlive() ? "" : "dead:", Math.sqrt(z.distanceToSqr(prey))));
                InvasionMod.LOGGER.info("SELFTEST hunt t={}s distances: {}", ticks / 20, sb);
            }
            if (ticks == 20 * 69) {
                var lv = prey.level();
                InvasionMod.LOGGER.info("SELFTEST prey {} on {} below2 {}", prey.blockPosition(), lv.getBlockState(prey.blockPosition().below()), lv.getBlockState(prey.blockPosition().below(2)));
                for (var z : HUNTERS) InvasionMod.LOGGER.info("SELFTEST hunter {} at {} onGround={} nav={} feetBlock={} target={}", z.getType().getDescriptionId(), z.blockPosition(), z.onGround(),
                        z.getNavigation().isInProgress(), lv.getBlockState(z.blockPosition()), z.getTarget() == prey);
            }
            if (ticks >= 20 * 70) e.getServer().halt(false);
        });
    }
}
