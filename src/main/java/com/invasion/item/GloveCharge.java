package com.invasion.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The Particle Gauntlet's three tanks and the selected element. Stored as an item data component. */
public record GloveCharge(int fire, int redstone, int water, int mode) {
    public static final int MAX = 300;
    public static final GloveCharge EMPTY = new GloveCharge(0, 0, 0, 0);

    public static final Codec<GloveCharge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("fire", 0).forGetter(GloveCharge::fire),
            Codec.INT.optionalFieldOf("redstone", 0).forGetter(GloveCharge::redstone),
            Codec.INT.optionalFieldOf("water", 0).forGetter(GloveCharge::water),
            Codec.INT.optionalFieldOf("mode", 0).forGetter(GloveCharge::mode)
    ).apply(i, GloveCharge::new));

    public static final StreamCodec<ByteBuf, GloveCharge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GloveCharge::fire,
            ByteBufCodecs.VAR_INT, GloveCharge::redstone,
            ByteBufCodecs.VAR_INT, GloveCharge::water,
            ByteBufCodecs.VAR_INT, GloveCharge::mode,
            GloveCharge::new);

    public Element element() {
        return Element.byIndex(mode);
    }

    public int get(Element e) {
        return switch (e) {
            case FIRE -> fire;
            case REDSTONE -> redstone;
            case WATER -> water;
        };
    }

    public GloveCharge with(Element e, int amount) {
        int v = Math.max(0, Math.min(MAX, amount));
        return switch (e) {
            case FIRE -> new GloveCharge(v, redstone, water, mode);
            case REDSTONE -> new GloveCharge(fire, v, water, mode);
            case WATER -> new GloveCharge(fire, redstone, v, mode);
        };
    }

    public GloveCharge withMode(Element e) {
        return new GloveCharge(fire, redstone, water, e.ordinal());
    }

    public enum Element {
        FIRE(0xFF8A1F, "fire"),
        REDSTONE(0xE02424, "redstone"),
        WATER(0x3B8BFF, "water");

        public final int color;
        public final String key;

        Element(int color, String key) {
            this.color = color;
            this.key = key;
        }

        public static Element byIndex(int i) {
            Element[] v = values();
            return v[Math.floorMod(i, v.length)];
        }

        public Element next() {
            return byIndex(ordinal() + 1);
        }
    }
}
