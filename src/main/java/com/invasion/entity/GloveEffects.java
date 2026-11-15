package com.invasion.entity;

import com.invasion.ModRegistry;
import com.invasion.item.GloveCharge.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.Predicate;

/**
 * The Particle Gauntlet's three elements, shared by the player's gauntlet and the Temple Guardian.
 * Fire burns, redstone hits hard and blasts targets away, water douses fire, shoves and jets the user backwards.
 */
public final class GloveEffects {
    public static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.4F);
    public static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.2F, 0.5F, 1.0F), 1.2F);
    public static final DustParticleOptions BLOOD_DROP = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.02F), 1.1F);

    private static DamageSource source(ServerLevel level, LivingEntity user) {
        return user instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(user);
    }

    private static Vec3 handPos(LivingEntity user, Vec3 dir) {
        return user.getEyePosition().add(dir.scale(0.8)).add(0, -0.3, 0);
    }

    /** Fire stream: a cone of flame. Damage only on the ticks that pass {@code damage > 0}. */
    public static void fireStream(ServerLevel level, LivingEntity user, Vec3 dir, double range, float damage, Predicate<LivingEntity> canHit) {
        RandomSource r = level.random;
        Vec3 from = handPos(user, dir);
        for (int i = 0; i < 6; i++) {
            Vec3 v = jitter(dir, r, 0.15).scale(0.6 + r.nextDouble() * 0.5);
            level.sendParticles(i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (damage <= 0) return;
        Vec3 end = clipEnd(level, user, from, dir, range);
        for (LivingEntity e : inBeam(level, user, from, end, 0.8, canHit)) {
            e.hurt(source(level, user), damage);
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 100));
            knock(e, dir, 0.15, 0.0);
        }
    }

    /** Redstone bolt: an instant crackling line that hits the first target hard and throws it back. */
    public static boolean redstoneBolt(ServerLevel level, LivingEntity user, Vec3 dir, double range, float damage, double knockback, Predicate<LivingEntity> canHit) {
        Vec3 from = handPos(user, dir);
        Vec3 end = clipEnd(level, user, from, dir, range);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, user, from, end,
                new AABB(from, end).inflate(1.0), e -> e instanceof LivingEntity le && le != user && le.isAlive() && canHit.test(le), 0.6F);
        if (hit != null) end = hit.getLocation();
        double len = end.distanceTo(from);
        for (double d = 0; d < len; d += 0.35) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(RED_DUST, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(), ModRegistry.GLOVE_REDSTONE.get(), SoundSource.PLAYERS, 0.8F, 1.3F + level.random.nextFloat() * 0.3F);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.hurt(source(level, user), damage);
            knock(target, dir, knockback, 0.35);
            level.sendParticles(RED_DUST, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0);
            return true;
        }
        return false;
    }

    /** Water jet: douses fire, pushes targets, and pushes the user the opposite way. */
    public static void waterJet(ServerLevel level, LivingEntity user, Vec3 dir, double range, float damage, double selfPush, Predicate<LivingEntity> canHit) {
        RandomSource r = level.random;
        Vec3 from = handPos(user, dir);
        for (int i = 0; i < 5; i++) {
            Vec3 v = jitter(dir, r, 0.08).scale(0.9 + r.nextDouble() * 0.4);
            level.sendParticles(i % 2 == 0 ? BLUE_DUST : ParticleTypes.SPLASH, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        Vec3 end = clipEnd(level, user, from, dir, range);
        // put out fires along the stream
        for (double d = 0; d < from.distanceTo(end) + 1; d += 1.0) {
            BlockPos p = BlockPos.containing(from.add(dir.scale(d)));
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                if (level.getBlockState(q).is(BlockTags.FIRE)) level.removeBlock(q, false);
            }
        }
        for (LivingEntity e : inBeam(level, user, from, end, 0.9, canHit)) {
            if (e.isOnFire()) e.clearFire();
            if (damage > 0) e.hurt(source(level, user), e.fireImmune() ? damage * 3 : damage);
            knock(e, dir, 0.35, 0.05);
        }
        if (selfPush > 0) {
            user.push(-dir.x * selfPush, -dir.y * selfPush * 0.8, -dir.z * selfPush);
            user.fallDistance = 0;
            user.hurtMarked = true;
        }
        if (user.isOnFire()) user.clearFire();
    }

    /** Shockwave in the given element: everything nearby is blasted outward. */
    public static void shockwave(ServerLevel level, LivingEntity user, Element element, double radius, float damage, Predicate<LivingEntity> canHit) {
        level.playSound(null, user.getX(), user.getY(), user.getZ(), ModRegistry.SHOCKWAVE.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        for (int i = 0; i < 48; i++) {
            float a = i * Mth.TWO_PI / 48;
            double vx = Mth.cos(a), vz = Mth.sin(a);
            switch (element) {
                case FIRE -> level.sendParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 0.3, user.getZ(), 0, vx, 0.02, vz, 0.5);
                case REDSTONE -> level.sendParticles(RED_DUST, user.getX() + vx * 1.5, user.getY() + 0.3, user.getZ() + vz * 1.5, 2, vx, 0.1, vz, 0);
                case WATER -> level.sendParticles(ParticleTypes.SPLASH, user.getX() + vx, user.getY() + 0.3, user.getZ() + vz, 3, vx * 0.5, 0.1, vz * 0.5, 0.3);
            }
            level.sendParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 0.2, user.getZ(), 0, vx, 0.03, vz, 0.5);
        }
        double push = element == Element.REDSTONE ? 2.6 : element == Element.WATER ? 1.8 : 1.3;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(radius),
                e -> e != user && e.isAlive() && canHit.test(e))) {
            Vec3 away = e.position().subtract(user.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
            e.hurt(source(level, user), element == Element.WATER ? damage * 0.5F : damage);
            if (element == Element.FIRE) e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 80));
            if (element == Element.WATER && e.isOnFire()) e.clearFire();
            knock(e, away.normalize(), push, 0.7);
        }
        if (element == Element.WATER) {
            BlockPos c = user.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
                if (level.getBlockState(q).is(BlockTags.FIRE)) level.removeBlock(q, false);
            }
        }
    }

    /** An elemental punch (left-click with a charged gauntlet). */
    public static void punch(ServerLevel level, LivingEntity user, LivingEntity target, Element element) {
        Vec3 dir = target.position().subtract(user.position()).multiply(1, 0, 1).normalize();
        switch (element) {
            case FIRE -> {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
                knock(target, dir, 0.3, 0.1);
            }
            case REDSTONE -> {
                target.hurt(source(level, user), 4.0F);
                level.sendParticles(RED_DUST, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.4, 0.3, 0);
                knock(target, dir, 1.6, 0.4);
            }
            case WATER -> {
                target.clearFire();
                level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
                knock(target, dir, 0.9, 0.2);
            }
        }
    }

    public static void knock(Entity e, Vec3 dir, double strength, double up) {
        e.push(dir.x * strength, up, dir.z * strength);
        e.hurtMarked = true;
    }

    private static Vec3 jitter(Vec3 dir, RandomSource r, double amount) {
        return dir.add((r.nextDouble() - 0.5) * amount, (r.nextDouble() - 0.5) * amount, (r.nextDouble() - 0.5) * amount).normalize();
    }

    private static Vec3 clipEnd(ServerLevel level, Entity user, Vec3 from, Vec3 dir, double range) {
        return level.clip(new ClipContext(from, from.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user)).getLocation();
    }

    private static java.util.List<LivingEntity> inBeam(ServerLevel level, LivingEntity user, Vec3 from, Vec3 end, double width, Predicate<LivingEntity> canHit) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.0), e -> {
            if (e == user || !e.isAlive() || !canHit.test(e)) return false;
            AABB box = e.getBoundingBox().inflate(width);
            return box.contains(from) || box.clip(from, end).isPresent();
        });
    }

    @SuppressWarnings("unused")
    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    private GloveEffects() {}
}
