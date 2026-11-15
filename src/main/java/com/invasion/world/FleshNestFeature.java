package com.invasion.world;

import com.invasion.ModFlesh;
import com.invasion.ModFluids;
import com.invasion.entity.FleshThingEntity;
import com.invasion.entity.FleshlingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Flesh Thing's nest: a pulsing dome of flesh with toothy maws around its entrance, a hollow chamber
 * floored with dark flesh, hair and a blood pool, the Flesh Thing at its heart and a few Fleshling guards.
 */
public class FleshNestFeature extends Feature<NoneFeatureConfiguration> {
    public FleshNestFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int cx = (o.getX() & ~15) + 8, cz = (o.getZ() & ~15) + 8;
        int cy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        if (cy <= level.getSeaLevel()) return false;
        BlockPos c = new BlockPos(cx, cy, cz);
        int R = 11;
        double ry = 0.75;

        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -3; dy <= R; dy++) {
                    double d = Math.sqrt(dx * dx + (dy / ry) * (dy / ry) + dz * dz);
                    BlockPos p = c.offset(dx, dy, dz);
                    if (dy < 0) {
                        // fill under the dome so it never floats
                        if (dx * dx + dz * dz <= R * R && level.getBlockState(p).canBeReplaced()) level.setBlock(p, flesh(r), 2);
                        continue;
                    }
                    if (d > R) continue;
                    if (d > R - 2) level.setBlock(p, flesh(r), 2);
                    else level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 2);
                }
                // dark flesh floor inside
                if (dx * dx + dz * dz < (R - 2) * (R - 2)) level.setBlock(c.offset(dx, -1, dz), ModFlesh.DARK_FLESH.get().defaultBlockState(), 2);
            }
        }

        // entrance tunnel with maws
        Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for (int i = R - 3; i <= R + 1; i++) {
            BlockPos mid = c.relative(door, i);
            for (int w = -2; w <= 2; w++) for (int y = 0; y <= 4; y++) {
                BlockPos p = mid.relative(door.getClockWise(), w).above(y);
                boolean edge = Math.abs(w) == 2 || y == 4;
                level.setBlock(p, edge ? (r.nextInt(3) == 0 ? ModFlesh.FLESH_MAW.get().defaultBlockState() : flesh(r)) : Blocks.CAVE_AIR.defaultBlockState(), 2);
            }
        }

        // hair and a blood pool inside
        for (int i = 0; i < 30; i++) {
            BlockPos p = c.offset(r.nextInt(15) - 7, 0, r.nextInt(15) - 7);
            if (level.isEmptyBlock(p) && p.distSqr(c) > 9)
                level.setBlock(p, (r.nextBoolean() ? ModFlesh.FLESH_HAIR : ModFlesh.LONG_FLESH_HAIR).get().defaultBlockState(), 2);
        }
        BlockPos pool = c.relative(door.getOpposite(), 5);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlock(pool.offset(dx, -1, dz), ModFluids.BLOOD_BLOCK.get().defaultBlockState(), 2);

        // the boss and its guards
        FleshThingEntity boss = ModFlesh.FLESH_THING.get().create(level.getLevel());
        if (boss != null) {
            boss.moveTo(cx + 0.5, cy, cz + 0.5, r.nextFloat() * 360, 0);
            boss.setPersistenceRequired();
            level.addFreshEntity(boss);
        }
        for (int i = 0; i < 3; i++) {
            FleshlingEntity f = ModFlesh.FLESHLING.get().create(level.getLevel());
            if (f == null) continue;
            float a = r.nextFloat() * Mth.TWO_PI;
            f.moveTo(cx + Mth.cos(a) * 5, cy, cz + Mth.sin(a) * 5, r.nextFloat() * 360, 0);
            f.finalizeSpawn(level, level.getCurrentDifficultyAt(f.blockPosition()), MobSpawnType.STRUCTURE, null);
            f.setPersistenceRequired();
            level.addFreshEntity(f);
        }
        return true;
    }

    private static BlockState flesh(RandomSource r) {
        int roll = r.nextInt(10);
        if (roll < 4) return ModFlesh.FLESH.get().defaultBlockState();
        if (roll < 7) return ModFlesh.FLESH_FOLDS.get().defaultBlockState();
        if (roll < 9) return ModFlesh.SPOTTED_FLESH.get().defaultBlockState();
        return ModFlesh.DARK_FLESH.get().defaultBlockState();
    }
}
