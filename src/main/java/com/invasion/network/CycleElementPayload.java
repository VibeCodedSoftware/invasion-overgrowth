package com.invasion.network;

import com.invasion.InvasionMod;
import com.invasion.ModRegistry;
import com.invasion.item.GloveCharge;
import com.invasion.item.ParticleGloveItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent when the player presses the "cycle element" key while holding the gauntlet. */
public record CycleElementPayload() implements CustomPacketPayload {
    public static final CycleElementPayload INSTANCE = new CycleElementPayload();
    public static final Type<CycleElementPayload> TYPE = new Type<>(InvasionMod.id("cycle_element"));
    public static final StreamCodec<ByteBuf, CycleElementPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleElementPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof ParticleGloveItem) {
                    GloveCharge charge = ParticleGloveItem.charge(stack);
                    GloveCharge.Element next = charge.element().next();
                    stack.set(ModRegistry.GLOVE_CHARGE.get(), charge.withMode(next));
                    player.displayClientMessage(Component.translatable("message.invasion.element",
                            Component.translatable("element.invasion." + next.key).withColor(next.color)), true);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
                    return;
                }
            }
        });
    }
}
