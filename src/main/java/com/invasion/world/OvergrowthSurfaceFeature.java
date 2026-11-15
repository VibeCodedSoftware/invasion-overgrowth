package com.invasion.world;

import com.invasion.InvasionMod;
import com.invasion.ModFlesh;
import com.invasion.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.material.Fluids;

/**
 * Runs once per chunk (only inside the chunk itself). In The Flesh / Dark Flesh it turns the whole ground into flesh / dark flesh
 * a few blocks deep; in The Flesh, the Blood Grove and the Bloodstone Wastes it turns surface water into real blood.
 */
public class OvergrowthSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<Biome> THE_FLESH = key("the_flesh");
    public static final ResourceKey<Biome> DARK_FLESH = key("dark_flesh");
    public static final ResourceKey<Biome> BLOOD_GROVE = key("blood_grove");
    public static final ResourceKey<Biome> BLOODSTONE = key("bloodstone_wastes");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, InvasionMod.id(name));
    }

    public OvergrowthSurfaceFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                Holder<Biome> biome = level.getBiome(p.set(x, floor, z));
                boolean dark = biome.is(DARK_FLESH);
                boolean flesh = dark || biome.is(THE_FLESH);
                if (!flesh && !biome.is(BLOOD_GROVE) && !biome.is(BLOODSTONE)) continue;

                // surface water -> blood
                for (int y = top; y > floor && y > top - 16; y--) {
                    p.set(x, y, z);
                    if (level.getFluidState(p).is(Fluids.WATER) && level.getBlockState(p).is(Blocks.WATER)) {
                        level.setBlock(p, level.getFluidState(p).isSource() ? ModFluids.BLOOD_BLOCK.get().defaultBlockState()
                                : ModFluids.BLOOD_BLOCK.get().defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, level.getBlockState(p).getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL)), 2);
                        changed = true;
                    }
                }
                if (!flesh) continue;

                // flesh ground: blotches of the four flesh blocks, plain flesh underneath
                int depth = 0;
                for (int y = floor; y > floor - 8 && depth < 4; y--) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !level.getFluidState(p).isEmpty()) break;
                    if (!replaceable(s)) break;
                    level.setBlock(p, (dark ? ModFlesh.DARK_FLESH : ModFlesh.FLESH).get().defaultBlockState(), 2);
                    depth++;
                    changed = true;
                }
                // grass and flowers left on top would float on flesh: clear plants above
                p.set(x, floor + 1, z);
                BlockState above = level.getBlockState(p);
                if (above.is(BlockTags.REPLACEABLE_BY_TREES) || above.is(BlockTags.FLOWERS) || above.is(Blocks.MOSS_CARPET)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        return changed;
    }

    private static boolean replaceable(BlockState s) {
        return s.is(BlockTags.MOSS_REPLACEABLE) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(Blocks.CLAY);
    }
}
