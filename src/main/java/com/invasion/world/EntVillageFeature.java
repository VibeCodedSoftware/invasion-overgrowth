package com.invasion.world;

import com.invasion.InvasionMod;
import com.invasion.ModRegistry;
import com.invasion.entity.EntEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * An Ent village: a clearing around a giant Elder Tree, a ring of mossy stone pillars with lanterns,
 * hollow-stump huts with moss roofs, a dirt path ring, flower gardens, and a few resident Ents.
 */
public class EntVillageFeature extends Feature<NoneFeatureConfiguration> {
    static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> ENT_VILLAGE_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, InvasionMod.id("chests/ent_village"));

    public EntVillageFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** Village radius: everything stays within 23 blocks of the chunk centre, i.e. inside the 3x3 chunks a feature may touch. */
    private static final int FLAT = 20, BLEND = 23, HOUSE_RING = 15;

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        int cy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        if (cy <= level.getSeaLevel() + 1) return false;
        // skip very uneven ground: the village needs a fairly level clearing
        int lo = cy, hi = cy;
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8;
            int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx + Math.round(Mth.cos(a) * 18), cz + Math.round(Mth.sin(a) * 18));
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        if (hi - lo > 10) return false;

        String biome = level.getBiome(new BlockPos(cx, cy, cz)).unwrapKey().map(k -> k.location().getPath()).orElse("");
        EntEntity.Kind kind = biome.contains("birch") ? EntEntity.Kind.BIRCH : biome.contains("azalea") ? EntEntity.Kind.AZALEA
                : biome.contains("oak") ? EntEntity.Kind.OAK : EntEntity.Kind.values()[r.nextInt(3)];
        BlockState wood = (kind == EntEntity.Kind.BIRCH ? Blocks.BIRCH_WOOD : Blocks.OAK_WOOD).defaultBlockState();
        BlockState leaves = (kind == EntEntity.Kind.BIRCH ? Blocks.BIRCH_LEAVES : kind == EntEntity.Kind.AZALEA ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.OAK_LEAVES)
                .defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);

        // Clearing: flat inside radius 20, easing back to the natural ground by radius 23 (no cliffs).
        for (int dx = -BLEND; dx <= BLEND; dx++) {
            for (int dz = -BLEND; dz <= BLEND; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > BLEND) continue;
                int x = cx + dx, z = cz + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                int groundY = d <= FLAT ? cy : (int) Math.round(Mth.lerp((d - FLAT) / (BLEND - FLAT), cy, top));
                if (d > FLAT && Math.abs(top - cy) <= 1) continue;
                for (int y = groundY; y < Math.max(top, groundY) + (d <= FLAT ? 16 : 3); y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                for (int y = top - 1; y < groundY - 1; y++) level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                BlockPos ground = new BlockPos(x, groundY - 1, z);
                BlockState surface;
                if (Math.abs(d - 9) < 0.8) surface = Blocks.DIRT_PATH.defaultBlockState();
                else surface = r.nextInt(4) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(ground, surface, 2);
                if (d > 10.5 && d < 19 && r.nextInt(7) == 0 && !surface.is(Blocks.DIRT_PATH)) {
                    BlockState flower = switch (r.nextInt(6)) {
                        case 0 -> Blocks.POPPY.defaultBlockState();
                        case 1 -> Blocks.OXEYE_DAISY.defaultBlockState();
                        case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                        case 3 -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
                        case 4 -> Blocks.AZALEA.defaultBlockState();
                        default -> Blocks.SHORT_GRASS.defaultBlockState();
                    };
                    level.setBlock(ground.above(), flower, 2);
                }
            }
        }

        // Three big hollow-stump houses (Ent sized: 9 wide inside, 3x6 doorway) facing the Elder Tree.
        float turn = r.nextFloat() * Mth.TWO_PI;
        float[] houseAngles = new float[3];
        for (int i = 0; i < 3; i++) {
            float a = turn + i * Mth.TWO_PI / 3;
            houseAngles[i] = a;
            int hx = cx + Math.round(Mth.cos(a) * HOUSE_RING), hz = cz + Math.round(Mth.sin(a) * HOUSE_RING);
            stumpHouse(level, r, new BlockPos(hx, cy, hz), cx - hx, cz - hz, wood, leaves, i == 0 || r.nextInt(2) == 0);
        }

        // Mossy standing stones with lanterns, leaving the paths to the doors open.
        for (int i = 0; i < 12; i++) {
            float a = i * Mth.TWO_PI / 12;
            boolean blocksDoor = false;
            for (float h : houseAngles) if (Math.abs(Mth.wrapDegrees((a - h) * Mth.RAD_TO_DEG)) < 22) blocksDoor = true;
            if (blocksDoor) continue;
            BlockPos base = new BlockPos(cx + Math.round(Mth.cos(a) * 11), cy, cz + Math.round(Mth.sin(a) * 11));
            int h = 2 + r.nextInt(2);
            for (int y = 0; y < h; y++) level.setBlock(base.above(y), (r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSSY_STONE_BRICKS).defaultBlockState(), 2);
            level.setBlock(base.above(h), i % 2 == 0 ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false) : Blocks.MOSS_CARPET.defaultBlockState(), 2);
        }

        // The Elder Tree at the centre.
        ResourceKey<ConfiguredFeature<?, ?>> treeKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, InvasionMod.id(switch (kind) {
            case OAK -> "giant_oak";
            case BIRCH -> "giant_birch";
            case AZALEA -> "giant_azalea";
        }));
        level.registryAccess().registry(Registries.CONFIGURED_FEATURE).flatMap(reg -> reg.getHolder(treeKey)).ifPresent(tree -> {
            BlockPos at = new BlockPos(cx, cy, cz);
            if (level.getBlockState(at.below()).is(BlockTags.DIRT)) tree.value().place(level, ctx.chunkGenerator(), r, at);
        });

        // Residents.
        int count = 3 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            EntEntity ent = ModRegistry.ENT.get().create(level.getLevel());
            if (ent == null) continue;
            float a = r.nextFloat() * Mth.TWO_PI;
            float dist = 7 + r.nextFloat() * 5;
            ent.moveTo(cx + Mth.cos(a) * dist, cy, cz + Mth.sin(a) * dist, r.nextFloat() * 360, 0);
            ent.finalizeSpawn(level, level.getCurrentDifficultyAt(ent.blockPosition()), MobSpawnType.STRUCTURE, null);
            ent.setKind(kind);
            ent.setPersistenceRequired();
            level.addFreshEntity(ent);
        }
        return true;
    }

    /**
     * A hollow tree-stump house sized for Ents (4.4 blocks tall): bark walls 9 high around a 9-wide room,
     * an arched 3x6 doorway, two small windows, a domed moss-and-leaf roof, lanterns on chains, and a chest.
     */
    private static void stumpHouse(WorldGenLevel level, RandomSource r, BlockPos c, int towardX, int towardZ,
                                   BlockState wood, BlockState leaves, boolean chest) {
        Direction door = Direction.getNearest(towardX, 0, towardZ);
        Direction side = door.getClockWise();
        final float OUTER = 5.3F, INNER = 4.3F;
        final int WALL = 9;
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > OUTER) continue;
            // foundation and floor
            level.setBlock(c.offset(dx, -1, dz), d > INNER || r.nextInt(3) != 0 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState(), 2);
            for (int y = -4; y < -1; y++) {
                BlockPos below = c.offset(dx, y, dz);
                if (level.getBlockState(below).canBeReplaced()) level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
            }
            int along = dx * door.getStepX() + dz * door.getStepZ();   // towards the door
            int across = dx * side.getStepX() + dz * side.getStepZ();
            int rim = WALL + (r.nextInt(3) == 0 ? 1 : 0);
            for (int y = 0; y <= rim; y++) {
                BlockPos p = c.offset(dx, y, dz);
                boolean wall = d > INNER;
                boolean doorway = along > 0 && Math.abs(across) <= 1 && (y < 5 || (y == 5 && across == 0));
                boolean window = along == 0 && Math.abs(across) >= 4 && (y == 4 || y == 5);
                if (wall && y < rim && !doorway && !window) level.setBlock(p, wood, 2);
                else if (y < WALL) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // domed roof: moss on top, leaves around the rim hanging over the walls
        for (int layer = 0; layer < 3; layer++) {
            float rad = 6.2F - layer * 1.8F;
            int R = (int) Math.ceil(rad);
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > rad) continue;
                BlockPos p = c.offset(dx, WALL + layer, dz);
                level.setBlock(p, d > rad - 1.2 ? leaves : Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                if (layer == 0 && d > 5.3 && r.nextInt(3) == 0) level.setBlock(p.below(), leaves, 2);
                if (layer == 2 && r.nextInt(3) == 0) level.setBlock(p.above(), (r.nextBoolean() ? Blocks.AZALEA : Blocks.MOSS_CARPET).defaultBlockState(), 2);
            }
        }
        // interior: lanterns on chains, a glow under the roof, mossy floor, a pot, a composter and the chest
        level.setBlock(c.above(WALL - 1), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
        for (int s = -1; s <= 1; s += 2) {
            BlockPos lamp = c.offset(side.getStepX() * 2 * s, 0, side.getStepZ() * 2 * s);
            for (int y = 6; y < WALL; y++) level.setBlock(lamp.above(y), Blocks.CHAIN.defaultBlockState(), 2);
            level.setBlock(lamp.above(5), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        }
        for (int i = 0; i < 6; i++) {
            BlockPos p = c.offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
            if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.MOSS_CARPET.defaultBlockState(), 2);
        }
        BlockPos back = c.offset(-door.getStepX() * 3, 0, -door.getStepZ() * 3);
        level.setBlock(back.relative(side, 2), Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 2);
        level.setBlock(back.relative(side, -2), Blocks.COMPOSTER.defaultBlockState(), 2);
        if (chest) {
            level.setBlock(back, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, door), 2);
            RandomizableContainer.setBlockEntityLootTable(level, r, back, ENT_VILLAGE_LOOT);
        }
        // vines creeping up the outside walls
        for (int i = 0; i < 14; i++) {
            float a = r.nextFloat() * Mth.TWO_PI;
            BlockPos p = c.offset(Math.round(Mth.cos(a) * 6), r.nextInt(WALL), Math.round(Mth.sin(a) * 6));
            Direction toWall = Direction.getNearest(c.getX() - p.getX(), 0, c.getZ() - p.getZ());
            if (level.getBlockState(p).isAir() && level.getBlockState(p.relative(toWall)).is(wood.getBlock())) {
                level.setBlock(p, Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(toWall), true), 2);
            }
        }
    }
}
