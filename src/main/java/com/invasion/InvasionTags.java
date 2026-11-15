package com.invasion;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class InvasionTags {
    public static final TagKey<Block> FLESH = TagKey.create(Registries.BLOCK, InvasionMod.id("flesh"));

    private InvasionTags() {}
}
