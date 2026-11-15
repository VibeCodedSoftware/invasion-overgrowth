package com.invasion.world;

import com.invasion.ModRegistry;
import com.invasion.entity.TempleGuardian;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Raises a mossy stepped-pyramid temple (96x96, 43 high, like the original Hostile Worlds temple) out of
 * vanilla mossy blocks on dry land in the Overgrowth, with a lit boss hall inside and fire braziers on top.
 */
public final class TempleBuilder {
    private static final int SIZE = 96, HEIGHT = 43, HALF = 48;
    private static final int HALL_MIN = 24, HALL_MAX = 71, HALL_TOP = 14;

    public static void ensureReady(ServerLevel level, InvasionData data) {
        if (data.templeCenter() == null) {
            BlockPos center = findSite(level);
            build(level, center);
            BlockPos portalBase = surface(level, center.getX(), center.getZ() - HALF - 18);
            PortalBuilder.buildPortal(level, portalBase);
            BlockPos arrival = surface(level, center.getX(), center.getZ() - HALF - 12);
            data.setTemple(center, arrival);
        }
        boolean invasionOn = data.state() == InvasionData.State.ACTIVE || data.state() == InvasionData.State.METEOR_FALLING;
        if (invasionOn && !data.bossSpawned()) {
            TempleGuardian boss = ModRegistry.GUARDIAN.get().create(level);
            if (boss != null) {
                BlockPos c = data.templeCenter();
                boss.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 180F, 0F);
                boss.setPersistenceRequired();
                level.addFreshEntity(boss);
                data.setBossSpawned(true);
            }
        }
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    /** Searches outward from spawn for dry, fairly flat land using the generator's noise (no chunk generation needed). */
    private static BlockPos findSite(ServerLevel level) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        int sea = gen.getSeaLevel();
        BlockPos fallback = null;
        for (int ring = 0; ring <= 20; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = dx * 64, cz = dz * 64;
                    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, centerH = 0;
                    int[][] samples = {{0, 0}, {-HALF, -HALF}, {HALF, -HALF}, {-HALF, HALF}, {HALF, HALF}, {0, -HALF - 18}};
                    for (int[] s : samples) {
                        int h = gen.getBaseHeight(cx + s[0], cz + s[1], Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                        if (s[0] == 0 && s[1] == 0) centerH = h;
                    }
                    if (!inWilds(gen, rs, cx, centerH, cz) || !inWilds(gen, rs, cx, centerH, cz - HALF - 18)) continue;
                    if (fallback == null && centerH > sea + 1) fallback = new BlockPos(cx, centerH, cz);
                    if (min > sea + 1 && max - min <= 14) return new BlockPos(cx, Math.min(centerH, min + 4), cz);
                }
            }
        }
        return fallback != null ? fallback : new BlockPos(0, sea + 2, 0);
    }

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> WILDS =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, com.invasion.InvasionMod.id("overgrown_wilds"));

    /** The temple only stands in the Overgrown Wilds (the mossy azalea biome). */
    private static boolean inWilds(ChunkGenerator gen, RandomState rs, int x, int y, int z) {
        return gen.getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(y),
                net.minecraft.core.QuartPos.fromBlock(z), rs.sampler()).is(WILDS);
    }

    private static void build(ServerLevel level, BlockPos center) {
        RandomSource r = RandomSource.create(center.asLong());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        int ox = center.getX() - HALF, oz = center.getZ() - HALF, baseY = center.getY();

        // Foundations under the footprint, and a gentle slope (not a vertical cut) blending the
        // surrounding terrain down to the temple's base level.
        final int BLEND = 14;
        for (int lx = -BLEND; lx < SIZE + BLEND; lx++) {
            for (int lz = -BLEND; lz < SIZE + BLEND; lz++) {
                int x = ox + lx, z = oz + lz;
                boolean inside = lx >= 0 && lz >= 0 && lx < SIZE && lz < SIZE;
                int inset = inside ? Math.min(Math.min(lx, lz), Math.min(SIZE - 1 - lx, SIZE - 1 - lz)) : -1;
                int outside = inside ? 0 : Math.max(Math.max(-lx, lx - (SIZE - 1)), Math.max(-lz, lz - (SIZE - 1)));
                // Highest terrain allowed in this column: the pyramid surface inside, a 1:1 slope rising outside.
                int maxTerrain = inside ? baseY + Math.min(inset, HEIGHT - 1) : baseY - 1 + outside;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                boolean cut = false;
                for (int y = Math.max(maxTerrain + 1, level.getMinBuildHeight()); y < Math.max(top, baseY + HEIGHT + 2); y++) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (!inside && s.getFluidState().isEmpty() == false) continue;
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), flags);
                    cut = true;
                }
                if (!inside && cut) {
                    // re-green the newly exposed ground
                    p.set(x, Math.min(maxTerrain, top - 1), z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.STONE) || s.is(Blocks.DIRT) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.ANDESITE) || s.is(Blocks.GRAVEL)
                            || s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE) || s.is(Blocks.TUFF)) {
                        level.setBlock(p, r.nextInt(3) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), flags);
                    }
                }
                // Raise low ground around the base into a mossy apron so the temple doesn't float.
                int fillDepth = inside ? 30 : Math.max(0, 6 - outside);
                for (int y = baseY - 1; y > baseY - 1 - fillDepth; y--) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.canBeReplaced() && s.getFluidState().isEmpty()) break;
                    level.setBlock(p, inside ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                            : (y == baseY - 1 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState()), flags);
                }
            }
        }

        for (int ly = 0; ly < HEIGHT; ly++) {
            int inset = ly;
            for (int lx = inset; lx < SIZE - inset; lx++) {
                for (int lz = inset; lz < SIZE - inset; lz++) {
                    p.set(ox + lx, baseY + ly, oz + lz);
                    boolean inHall = ly >= 1 && ly <= HALL_TOP && lx >= HALL_MIN && lx <= HALL_MAX && lz >= HALL_MIN && lz <= HALL_MAX;
                    boolean inTunnel = ly >= 1 && ly <= 6 && lx >= 45 && lx <= 50 && lz < HALL_MIN;
                    if (inHall || inTunnel) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), flags);
                        continue;
                    }
                    boolean hallFloor = ly == 0 && lx >= HALL_MIN && lx <= HALL_MAX && lz >= HALL_MIN && lz <= HALL_MAX;
                    level.setBlock(p, hallFloor ? floorBlock(lx, lz, r) : shellBlock(r), flags);
                    boolean ledge = lx == inset || lz == inset || lx == SIZE - 1 - inset || lz == SIZE - 1 - inset;
                    if (ledge && ly < HEIGHT - 1) {
                        int roll = r.nextInt(10);
                        if (roll < 2) level.setBlock(p.above(), Blocks.MOSS_CARPET.defaultBlockState(), flags);
                        else if (roll == 2) level.setBlock(p.above(), Blocks.AZALEA.defaultBlockState(), flags);
                    }
                }
            }
        }

        int[] cols = {30, 40, 55, 65};
        for (int px : cols) for (int pz : cols) for (int ly = 1; ly <= HALL_TOP; ly++) for (int ox2 = 0; ox2 < 2; ox2++) for (int oz2 = 0; oz2 < 2; oz2++) {
            p.set(ox + px + ox2, baseY + ly, oz + pz + oz2);
            BlockState s = (ly == 4 || ly == 10) ? Blocks.VERDANT_FROGLIGHT.defaultBlockState()
                    : (r.nextInt(5) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
            level.setBlock(p, s, flags);
        }
        for (int lz = 2; lz < HALL_MIN; lz += 4) {
            level.setBlock(p.set(ox + 45, baseY + 4, oz + lz), Blocks.VERDANT_FROGLIGHT.defaultBlockState(), flags);
            level.setBlock(p.set(ox + 50, baseY + 4, oz + lz), Blocks.VERDANT_FROGLIGHT.defaultBlockState(), flags);
        }
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2 / 32;
            int lx = 48 + (int) Math.round(Math.cos(a) * 12), lz = 48 + (int) Math.round(Math.sin(a) * 12);
            level.setBlock(p.set(ox + lx, baseY, oz + lz), Blocks.SHROOMLIGHT.defaultBlockState(), flags);
        }
        int top = baseY + HEIGHT;
        int[][] braziers = {{43, 43}, {43, 52}, {52, 43}, {52, 52}};
        for (int[] b : braziers) {
            level.setBlock(p.set(ox + b[0], top, oz + b[1]), Blocks.NETHERRACK.defaultBlockState(), flags);
            level.setBlock(p.set(ox + b[0], top + 1, oz + b[1]), Blocks.FIRE.defaultBlockState(), flags);
        }
    }

    private static BlockState shellBlock(RandomSource r) {
        int roll = r.nextInt(100);
        if (roll < 50) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (roll < 75) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (roll < 85) return Blocks.MOSS_BLOCK.defaultBlockState();
        if (roll < 93) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }

    private static BlockState floorBlock(int lx, int lz, RandomSource r) {
        if ((lx + lz) % 8 == 0) return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        return r.nextInt(3) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    }

    private TempleBuilder() {}
}
