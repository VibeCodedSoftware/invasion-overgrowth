package com.invasion.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A towering giant: 2x2 trunk 40-58 tall wrapped in vines, tiers of long horizontal branches that taper toward
 * the top (a tiered cone), leaf clusters along every branch with a big tuft at each tip, a leafy spire, and
 * thick roots spreading across the ground. Stays within the 3x3 chunk area features may write to.
 */
public class GiantTreeFeature extends Feature<GiantTreeFeature.Config> {
    public record Config(BlockState log, BlockState leaves, BlockState accentLeaves) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("leaves").forGetter(Config::leaves),
                BlockState.CODEC.fieldOf("accent_leaves").forGetter(Config::accentLeaves)
        ).apply(i, Config::new));
    }

    public GiantTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        Config cfg = ctx.config();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT) && !level.getBlockState(base.below()).isSolid()) return false;
        int height = 40 + r.nextInt(19);
        if (base.getY() + height + 4 >= level.getMaxBuildHeight()) return false;
        // need open space at the base
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
            BlockState s = level.getBlockState(base.offset(dx, 1, dz));
            if (!s.canBeReplaced() && !s.is(BlockTags.LEAVES)) return false;
        }

        BlockState logY = cfg.log().hasProperty(RotatedPillarBlock.AXIS) ? cfg.log().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : cfg.log();

        // Trunk: 2x2 for most of the height, 1x1 near the top.
        int thinFrom = height - 7;
        for (int y = -1; y < height; y++) {
            for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
                if (y >= thinFrom && (dx == 1 || dz == 1)) continue;
                setLog(level, base.offset(dx, y, dz), logY);
            }
        }

        // Roots radiating along the ground, splitting at the ends.
        int roots = 4 + r.nextInt(3);
        float rootAngle = r.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < roots; i++) {
            float a = rootAngle + i * Mth.TWO_PI / roots + (r.nextFloat() - 0.5F) * 0.3F;
            int len = 7 + r.nextInt(6);
            BlockPos end = line(level, base.offset(1, 0, 1), a, len, cfg.log(), true);
            for (int s = -1; s <= 1; s += 2) line(level, end, a + s * 0.7F, 2 + r.nextInt(3), cfg.log(), true);
        }

        // Branch tiers: long at the bottom, short at the top.
        int firstTier = 8 + r.nextInt(3);
        float spin = r.nextFloat() * Mth.TWO_PI;
        for (int y = firstTier; y < height - 4; y += 3 + r.nextInt(2)) {
            float t = (float) (y - firstTier) / (height - firstTier);
            int maxLen = Math.round(Mth.lerp(t, 12, 3));
            int count = t < 0.5F ? 6 : 4;
            spin += 0.6F + r.nextFloat() * 0.4F;
            for (int i = 0; i < count; i++) {
                float a = spin + i * Mth.TWO_PI / count + (r.nextFloat() - 0.5F) * 0.35F;
                int len = Math.max(2, maxLen - r.nextInt(3));
                BlockPos start = base.offset(r.nextBoolean() ? 1 : 0, y, r.nextBoolean() ? 1 : 0);
                BlockPos tip = branchWithLeaves(level, r, start, a, len, cfg);
                blob(level, r, tip, 2.2F + (1 - t), 1.4F, cfg);
            }
        }
        // Crown spire.
        BlockPos top = base.above(height);
        for (int y = -4; y <= 3; y++) {
            float rad = y < 0 ? 2.5F : 2.5F - y * 0.7F;
            disc(level, r, top.above(y), rad, cfg);
        }
        if (canWrite(level, top.above(4))) level.setBlock(top.above(4), cfg.leaves(), 2);

        // Vines spiralling up the trunk.
        for (int y = 2; y < thinFrom; y++) {
            Direction side = Direction.from2DDataValue((y / 4) % 4);
            int ox = side == Direction.EAST ? 2 : side == Direction.WEST ? -1 : (y % 2);
            int oz = side == Direction.SOUTH ? 2 : side == Direction.NORTH ? -1 : (y % 2);
            BlockPos v = base.offset(ox, y, oz);
            if (canWrite(level, v) && level.isEmptyBlock(v) && r.nextInt(3) > 0) {
                BooleanPropertyFacing face = BooleanPropertyFacing.of(side.getOpposite());
                level.setBlock(v, Blocks.VINE.defaultBlockState().setValue(face.prop, true), 2);
            }
        }
        return true;
    }

    private record BooleanPropertyFacing(net.minecraft.world.level.block.state.properties.BooleanProperty prop) {
        static BooleanPropertyFacing of(Direction d) {
            return new BooleanPropertyFacing(VineBlock.PROPERTY_BY_DIRECTION.get(d));
        }
    }

    /** During world generation a feature may only write within one chunk of the chunk being decorated. */
    static boolean canWrite(WorldGenLevel level, BlockPos p) {
        if (!(level instanceof net.minecraft.server.level.WorldGenRegion region)) return true;
        net.minecraft.world.level.ChunkPos c = region.getCenter();
        return Math.abs((p.getX() >> 4) - c.x) <= 1 && Math.abs((p.getZ() >> 4) - c.z) <= 1;
    }

    private static void setLog(WorldGenLevel level, BlockPos p, BlockState log) {
        if (!canWrite(level, p)) return;
        BlockState s = level.getBlockState(p);
        if (s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.DIRT) || s.is(Blocks.VINE)) level.setBlock(p, log, 2);
    }

    /** Horizontal log line at an angle; roots follow the ground down. Returns the end position. */
    private static BlockPos line(WorldGenLevel level, BlockPos from, float angle, int len, BlockState log, boolean root) {
        Direction.Axis axis = Math.abs(Mth.cos(angle)) > Math.abs(Mth.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState s = log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log;
        BlockPos p = from;
        for (int i = 1; i <= len; i++) {
            p = from.offset(Math.round(Mth.cos(angle) * i), 0, Math.round(Mth.sin(angle) * i));
            if (root) {
                int y = p.getY();
                while (y > from.getY() - 4 && level.getBlockState(new BlockPos(p.getX(), y - 1, p.getZ())).canBeReplaced()) y--;
                p = new BlockPos(p.getX(), y, p.getZ());
            }
            setLog(level, p, s);
        }
        return p;
    }

    private static BlockPos branchWithLeaves(WorldGenLevel level, RandomSource r, BlockPos from, float angle, int len, Config cfg) {
        Direction.Axis axis = Math.abs(Mth.cos(angle)) > Math.abs(Mth.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState s = cfg.log().hasProperty(RotatedPillarBlock.AXIS) ? cfg.log().setValue(RotatedPillarBlock.AXIS, axis) : cfg.log();
        BlockPos p = from;
        for (int i = 1; i <= len; i++) {
            p = from.offset(Math.round(Mth.cos(angle) * i), i > len * 0.7 ? 1 : 0, Math.round(Mth.sin(angle) * i));
            setLog(level, p, s);
            if (i > 2 && i % 2 == 0) blob(level, r, p.above(), 1.6F, 0.8F, cfg);
        }
        return p;
    }

    private static void blob(WorldGenLevel level, RandomSource r, BlockPos c, float rad, float yScale, Config cfg) {
        int ir = (int) Math.ceil(rad);
        for (int dx = -ir; dx <= ir; dx++) for (int dy = -1; dy <= Math.ceil(rad * yScale); dy++) for (int dz = -ir; dz <= ir; dz++) {
            double d = dx * dx + (dy / yScale) * (dy / yScale) + dz * dz;
            if (d > rad * rad || (d > (rad - 1) * (rad - 1) && r.nextInt(3) == 0)) continue;
            leaf(level, r, c.offset(dx, dy, dz), cfg);
        }
    }

    private static void disc(WorldGenLevel level, RandomSource r, BlockPos c, float rad, Config cfg) {
        int ir = (int) Math.ceil(rad);
        for (int dx = -ir; dx <= ir + 1; dx++) for (int dz = -ir; dz <= ir + 1; dz++) {
            double ddx = dx - 0.5, ddz = dz - 0.5;
            if (ddx * ddx + ddz * ddz <= rad * rad) leaf(level, r, c.offset(dx, 0, dz), cfg);
        }
    }

    private static void leaf(WorldGenLevel level, RandomSource r, BlockPos p, Config cfg) {
        if (canWrite(level, p) && (level.isEmptyBlock(p) || level.getBlockState(p).is(Blocks.VINE))) {
            level.setBlock(p, r.nextInt(5) == 0 ? cfg.accentLeaves() : cfg.leaves(), 2);
        }
    }
}
