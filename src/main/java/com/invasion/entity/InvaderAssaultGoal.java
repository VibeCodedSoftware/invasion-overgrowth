package com.invasion.entity;

import com.invasion.InvasionConfig;
import com.invasion.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Chase-and-attack with swords; when the target can't be reached by walking (or the zombie is stuck),
 * it tunnels toward the target: straight through walls, staircasing up or down, or pillaring up.
 */
public class InvaderAssaultGoal extends Goal {
    /** Beyond this horizontal distance the invader travels in legs instead of pathing straight to the target. */
    private static final double FAR = 36.0;
    private final InvaderZombie mob;
    private int repathTimer;
    private int attackCooldown;
    private int stuckTicks;
    private boolean digging;
    private Vec3 lastPos = Vec3.ZERO;

    @Nullable private BlockPos miningPos;
    private int mineProgress;
    private int mineTime;

    @Nullable private BlockPos pillarBase;

    public InvaderAssaultGoal(InvaderZombie mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) return false;
        return !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    @Override
    public void start() {
        repathTimer = 0;
        stuckTicks = 0;
        digging = false;
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        resetMining();
        pillarBase = null;
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (attackCooldown > 0) attackCooldown--;
        if (attackCooldown <= 0 && mob.isWithinMeleeAttackRange(target) && mob.getSensing().hasLineOfSight(target)) {
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
            attackCooldown = 20;
        }

        // Stuck detection: hardly moved for 3 seconds while still away from the target.
        if (mob.tickCount % 20 == 0) {
            boolean barelyMoved = mob.position().distanceToSqr(lastPos) < 0.3;
            if (barelyMoved && mob.distanceToSqr(target) > 4.0) stuckTicks += 20;
            else stuckTicks = Math.max(0, stuckTicks - 20);
            lastPos = mob.position();
        }

        if (--repathTimer <= 0) {
            repathTimer = 10 + mob.getRandom().nextInt(10);
            double fx = target.getX() - mob.getX(), fz = target.getZ() - mob.getZ();
            if (fx * fx + fz * fz > FAR * FAR && stuckTicks < 60) {
                // Far away (a hunter tracking you across the map): travel in 24-block legs toward you.
                double len = Math.sqrt(fx * fx + fz * fz);
                int wx = net.minecraft.util.Mth.floor(mob.getX() + fx / len * 24), wz = net.minecraft.util.Mth.floor(mob.getZ() + fz / len * 24);
                Level level = mob.level();
                int wy = level.hasChunkAt(new BlockPos(wx, mob.getBlockY(), wz))
                        ? level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz) : mob.getBlockY();
                Path leg = mob.getNavigation().createPath(wx + 0.5, wy, wz + 0.5, 1);
                if (leg != null && leg.getNodeCount() > 1) {
                    digging = false;
                    mob.getNavigation().moveTo(leg, 1.15);
                } else {
                    digging = canDig();
                    if (!digging) mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.15);
                }
                if (digging) digTowards(target);
                return;
            }
            Path path = mob.getNavigation().createPath(target, 0);
            boolean reachable = path != null && path.canReach();
            if (reachable && stuckTicks < 60) {
                digging = false;
                mob.getNavigation().moveTo(path, 1.0);
            } else {
                digging = canDig();
                if (!digging) mob.getNavigation().moveTo(target, 1.0);
            }
        }

        if (digging) digTowards(target);
    }

    private boolean canDig() {
        return InvasionConfig.INVADERS_MINE.get() && mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private void digTowards(LivingEntity target) {
        Level level = mob.level();
        mob.getNavigation().stop();
        BlockPos next = chooseBlockToMine(target);

        if (next == null) {
            resetMining();
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            if (mob.horizontalCollision && mob.onGround()) mob.getJumpControl().jump();
            tryPillar(target);
            return;
        }

        if (!next.equals(miningPos)) {
            resetMining();
            miningPos = next;
            float hardness = level.getBlockState(next).getDestroySpeed(level, next);
            mineTime = Math.max(8, (int) (hardness * 25 / mob.digSpeed()));
        }
        mob.getLookControl().setLookAt(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
        mineProgress++;
        if (mineProgress % 6 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
            BlockState s = level.getBlockState(next);
            level.playSound(null, next, s.getSoundType().getHitSound(), SoundSource.HOSTILE, 0.6F, 0.8F);
        }
        level.destroyBlockProgress(mob.getId(), next, Math.min(9, mineProgress * 10 / mineTime));
        if (mineProgress >= mineTime) {
            level.destroyBlock(next, true, mob);
            level.destroyBlockProgress(mob.getId(), next, -1);
            miningPos = null;
            mineProgress = 0;
            stuckTicks = 0;
            repathTimer = 0;
        }
    }

    @Nullable
    private BlockPos chooseBlockToMine(LivingEntity target) {
        BlockPos feet = mob.blockPosition();
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double dy = target.getY() - mob.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        List<BlockPos> candidates = new ArrayList<>();
        if (horizontal < 1.2) {
            if (dy > 1.5) candidates.add(feet.above(2));
            else if (dy < -1.5) candidates.add(feet.below());
        } else {
            Direction dir = Direction.getNearest(dx, 0, dz);
            BlockPos front = feet.relative(dir);
            if (dy > 1.5) {
                // Staircase up: clear headroom, keep the block in front as a step.
                candidates.add(feet.above(2));
                candidates.add(front.above());
                candidates.add(front.above(2));
            } else if (dy < -1.5) {
                // Staircase down.
                candidates.add(front.above());
                candidates.add(front);
                candidates.add(front.below());
            } else {
                candidates.add(front);
                candidates.add(front.above());
            }
        }
        for (BlockPos p : candidates) if (isMineable(p)) return p;
        return null;
    }

    private boolean isMineable(BlockPos p) {
        Level level = mob.level();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty()) return false;
        if (s.is(ModRegistry.PORTAL_FRAME.get()) || s.is(ModRegistry.PORTAL.get())) return false;
        float hardness = s.getDestroySpeed(level, p);
        return hardness >= 0 && hardness <= 30; // obsidian and tougher stops them
    }

    /** Target is right above: jump and place mossy cobblestone underneath, nerd-pole style. */
    private void tryPillar(LivingEntity target) {
        Level level = mob.level();
        double dy = target.getY() - mob.getY();
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        // Swimming below a raised target (tree, pillar, cliff by a lake): block up out of the water.
        if (mob.isInWater() && dy >= 1.0 && dx * dx + dz * dz <= 36.0 && mob.tickCount % 8 == 0) {
            BlockPos feet = mob.blockPosition();
            if (level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER) && level.getBlockState(feet).canBeReplaced()
                    && level.getBlockState(feet.above(2)).getCollisionShape(level, feet.above(2)).isEmpty()) {
                level.setBlock(feet, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
                mob.setPos(mob.getX(), feet.getY() + 1.0, mob.getZ());
            }
            return;
        }
        if (dy < 2.5 || dx * dx + dz * dz > 4.0) {
            pillarBase = null;
            return;
        }
        if (pillarBase == null) {
            if (mob.onGround() && level.getBlockState(mob.blockPosition().above(2)).getCollisionShape(level, mob.blockPosition().above(2)).isEmpty()) {
                pillarBase = mob.blockPosition();
                mob.getJumpControl().jump();
            }
        } else if (mob.getY() > pillarBase.getY() + 1.05 && level.getBlockState(pillarBase).canBeReplaced()) {
            level.setBlock(pillarBase, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
            pillarBase = null;
        } else if (mob.onGround() && mob.getY() < pillarBase.getY() + 0.5 && mob.tickCount % 10 == 0) {
            pillarBase = null;
        }
    }

    private void resetMining() {
        if (miningPos != null) mob.level().destroyBlockProgress(mob.getId(), miningPos, -1);
        miningPos = null;
        mineProgress = 0;
    }
}
