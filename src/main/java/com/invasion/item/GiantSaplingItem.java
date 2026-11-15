package com.invasion.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/** Rare Ent drop: shimmers like an enchanted item and grows into a giant tree. */
public class GiantSaplingItem extends BlockItem {
    public GiantSaplingItem(Block block) {
        super(block, new Properties().rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
