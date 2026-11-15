package com.invasion.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Hard-hat invader with a pickaxe: tunnels, staircases and pillars toward you far faster than the swordsmen. */
public class InvaderMiner extends InvaderZombie {

    public InvaderMiner(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public float digSpeed() {
        return 1.0F;
    }

    @Override
    public void equipForInvasion(RandomSource r) {
        int roll = r.nextInt(100);
        Item pick = roll < 35 ? Items.WOODEN_PICKAXE : roll < 75 ? Items.STONE_PICKAXE : roll < 95 ? Items.IRON_PICKAXE : Items.DIAMOND_PICKAXE;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pick));
        setDropChance(EquipmentSlot.MAINHAND, 0.08F);
    }
}
