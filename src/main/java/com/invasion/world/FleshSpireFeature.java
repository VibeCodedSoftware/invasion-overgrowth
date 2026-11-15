package com.invasion.world;

import com.invasion.ModFlesh;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Spotted-flesh rock formations: leaning tapered spires, lumpy boulder clusters and the odd arch. Radius <= 5. */
public class FleshSpireFeature extends Feature<NoneFeatureConfiguration> {
    public FleshSpireFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).isSolid()) return false;
        int kind = r.nextInt(10);
        if (kind < 7) boulders(level, r, o);
        else if (kind < 9) spire(level, r, o);
        else arch(level, r, o);
        return true;
    }

    private void spire(WorldGenLevel level, RandomSource r, BlockPos o) {
        int h = 7 + r.nextInt(9);
        float base = 1.8F + r.nextFloat() * 1.4F;
        float lx = (r.nextFloat() - 0.5F) * 0.5F, lz = (r.nextFloat() - 0.5F) * 0.5F;
        for (int y = -3; y < h; y++) {
            float t = Math.max(0, y) / (float) h;
            float rad = base * (1 - t * 0.85F) + 0.4F;
            float cx = lx * y, cz = lz * y;
            blob(level, r, o.offset(Math.round(cx), y, Math.round(cz)), rad, 1);
        }
    }

    private void boulders(WorldGenLevel level, RandomSource r, BlockPos o) {
        int n = 2 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            BlockPos c = o.offset(r.nextInt(5) - 2, r.nextInt(2), r.nextInt(5) - 2);
            float rad = 1.3F + r.nextFloat() * 1.5F;
            for (int y = -2; y <= Mth.ceil(rad); y++) blob(level, r, c.above(y), (float) Math.sqrt(Math.max(0, rad * rad - y * y)) + 0.3F, 1);
        }
    }

    private void arch(WorldGenLevel level, RandomSource r, BlockPos o) {
        boolean alongX = r.nextBoolean();
        int span = 3 + r.nextInt(2), h = 5 + r.nextInt(3);
        for (int i = -span; i <= span; i++) {
            double a = (i + span) / (double) (2 * span) * Math.PI;
            int y = (int) Math.round(Math.sin(a) * h);
            BlockPos p = alongX ? o.offset(i, y, 0) : o.offset(0, y, i);
            blob(level, r, p, 1.3F, 1);
            if (Math.abs(i) == span) for (int d = -2; d < y; d++) blob(level, r, alongX ? o.offset(i, d, 0) : o.offset(0, d, i), 1.4F, 1);
        }
    }

    /** A flat disc of flesh rock at one height. */
    private void blob(WorldGenLevel level, RandomSource r, BlockPos c, float rad, int unused) {
        int R = Mth.ceil(rad);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            if (dx * dx + dz * dz > rad * rad + r.nextFloat() * 0.6F) continue;
            BlockPos p = c.offset(dx, 0, dz);
            BlockState s = level.getBlockState(p);
            if (s.getDestroySpeed(level, p) < 0) continue;
            level.setBlock(p, rock(r), 2);
        }
    }

    private static BlockState rock(RandomSource r) {
        return ModFlesh.SPOTTED_FLESH.get().defaultBlockState();
    }
}
