package com.invasion.item;

import com.invasion.ModRegistry;
import com.invasion.entity.GloveEffects;
import com.invasion.item.GloveCharge.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Particle Gauntlet. Three tanks (fire, redstone, water) are charged by holding right-click while
 * looking at a torch/fire, a redstone torch/wire/block, or water. Holding right-click elsewhere fires the
 * selected element; sneak + right-click releases an elemental shockwave; punching with a charged gauntlet
 * adds an elemental hit. Press the "cycle element" key (default R) to switch elements.
 */
public class ParticleGloveItem extends Item {
    private static final int CHARGE_PER_TICK = 5;
    private static final int SHOCKWAVE_COST = 50;
    private static final int PUNCH_COST = 15;

    public ParticleGloveItem(Properties props) {
        super(props);
    }

    public static GloveCharge charge(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.GLOVE_CHARGE.get(), GloveCharge.EMPTY);
    }

    private static void setCharge(ItemStack stack, GloveCharge c) {
        stack.set(ModRegistry.GLOVE_CHARGE.get(), c);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            GloveCharge c = charge(stack);
            Element el = c.element();
            if (c.get(el) < SHOCKWAVE_COST) {
                if (!level.isClientSide) emptyFizzle(level, player);
                return InteractionResultHolder.fail(stack);
            }
            if (level instanceof ServerLevel server) {
                GloveEffects.shockwave(server, player, el, 6.0, 7.0F, e -> true);
                setCharge(stack, c.with(el, c.get(el) - SHOCKWAVE_COST));
            }
            player.getCooldowns().addCooldown(this, 50);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) return;
        int used = getUseDuration(stack, user) - remaining;
        GloveCharge c = charge(stack);
        boolean creative = false; // every tank must be charged, even in creative

        // 1) Charging: look at an element source while holding use.
        SourceHit src = findSource(level, user);
        if (src != null) {
            Element el = src.element;
            if (c.get(el) < GloveCharge.MAX) {
                setCharge(stack, c.with(el, c.get(el) + CHARGE_PER_TICK).withMode(el));
                pullParticles(server, user, src.pos, el);
                if (used % 8 == 0) {
                    level.playSound(null, user.getX(), user.getY(), user.getZ(), ModRegistry.GLOVE_CHARGE_SOUND.get(), SoundSource.PLAYERS,
                            0.5F, 0.8F + c.get(el) / (float) GloveCharge.MAX * 0.8F);
                }
            }
            return;
        }

        // 2) Firing the selected element.
        Element el = c.element();
        int amount = c.get(el);
        if (amount <= 0 && !creative) {
            if (used % 10 == 0) emptyFizzle(level, user);
            return;
        }
        Vec3 dir = user.getLookAngle();
        int cost = 0;
        switch (el) {
            case FIRE -> {
                GloveEffects.fireStream(server, user, dir, 12.0, used % 5 == 0 ? 4.0F : 0.0F, e -> true);
                if (used % 16 == 0) level.playSound(null, user.getX(), user.getY(), user.getZ(), ModRegistry.GLOVE_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                cost = 2;
            }
            case REDSTONE -> {
                if (used % 6 == 0) {
                    GloveEffects.redstoneBolt(server, user, dir, 24.0, 6.0F, 1.4, e -> true);
                    cost = 12;
                }
            }
            case WATER -> {
                GloveEffects.waterJet(server, user, dir, 10.0, used % 10 == 0 ? 2.0F : 0.0F, 0.11, e -> true);
                if (used % 12 == 0) level.playSound(null, user.getX(), user.getY(), user.getZ(), ModRegistry.GLOVE_WATER.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
                cost = 1;
            }
        }
        if (!creative && cost > 0) setCharge(stack, c.with(el, amount - cost));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel server) {
            GloveCharge c = charge(stack);
            Element el = c.element();
            boolean creative = false;
            if (c.get(el) >= PUNCH_COST || creative) {
                GloveEffects.punch(server, attacker, target, el);
                if (!creative) setCharge(stack, c.with(el, c.get(el) - PUNCH_COST));
            }
        }
        return true;
    }

    private record SourceHit(Element element, BlockPos pos) {}

    @Nullable
    private static SourceHit findSource(Level level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 end = eye.add(user.getLookAngle().scale(5.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, user));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        BlockState s = level.getBlockState(pos);
        if (level.getFluidState(pos).is(FluidTags.WATER) || s.is(Blocks.WATER_CAULDRON)) return new SourceHit(Element.WATER, pos);
        if (level.getFluidState(pos).is(FluidTags.LAVA) || s.is(BlockTags.FIRE) || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)
                || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)
                || s.is(Blocks.MAGMA_BLOCK) || (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT))) {
            return new SourceHit(Element.FIRE, pos);
        }
        if (s.is(Blocks.REDSTONE_BLOCK) || s.is(Blocks.REDSTONE_WIRE) || s.is(Blocks.REDSTONE_ORE) || s.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || (s.getBlock() instanceof RedstoneTorchBlock && s.getValue(BlockStateProperties.LIT))
                || (s.is(Blocks.REDSTONE_LAMP) && s.getValue(BlockStateProperties.LIT))) {
            return new SourceHit(Element.REDSTONE, pos);
        }
        return null;
    }

    private static void pullParticles(ServerLevel level, LivingEntity user, BlockPos from, Element el) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 hand = user.getEyePosition().add(user.getLookAngle().scale(0.6)).add(0, -0.35, 0);
        Vec3 delta = hand.subtract(start);
        ParticleOptions p = switch (el) {
            case FIRE -> ParticleTypes.FLAME;
            case REDSTONE -> GloveEffects.RED_DUST;
            case WATER -> GloveEffects.BLUE_DUST;
        };
        for (int i = 0; i < 4; i++) {
            double f = level.random.nextDouble();
            Vec3 at = start.add(delta.scale(f));
            level.sendParticles(p, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private static void emptyFizzle(Level level, Entity user) {
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.8F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide || !selected || !(entity instanceof LivingEntity user)) return;
        GloveCharge c = charge(stack);
        Element el = c.element();
        if (c.get(el) <= 0 || level.random.nextInt(4) != 0) return;
        float yaw = user.getYRot() * Mth.DEG_TO_RAD;
        double x = user.getX() - Mth.cos(yaw) * 0.45, z = user.getZ() - Mth.sin(yaw) * 0.45, y = user.getY() + 0.9;
        ParticleOptions p = switch (el) {
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case REDSTONE -> GloveEffects.RED_DUST;
            case WATER -> ParticleTypes.DRIPPING_WATER;
        };
        level.addParticle(p, x, y, z, 0, 0.01, 0);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (user instanceof Player player) player.getCooldowns().addCooldown(this, 8);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        GloveCharge c = charge(stack);
        return Math.round(13.0F * c.get(c.element()) / GloveCharge.MAX);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return charge(stack).element().color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GloveCharge c = charge(stack);
        for (Element el : Element.values()) {
            Component name = Component.translatable("element.invasion." + el.key).withColor(el.color);
            Component line = Component.literal(el == c.element() ? "> " : "  ").append(name)
                    .append(Component.literal(": " + c.get(el) + " / " + GloveCharge.MAX).withStyle(ChatFormatting.GRAY));
            tooltip.add(line);
        }
        tooltip.add(Component.translatable("item.invasion.particle_glove.tip_charge").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.invasion.particle_glove.tip_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
