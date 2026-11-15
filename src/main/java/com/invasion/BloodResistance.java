package com.invasion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** The Blood Resistance boots enchantment (treasure only: dungeon and Ent village chests). */
public final class BloodResistance {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, InvasionMod.id("blood_resistance"));

    public static boolean has(LivingEntity entity) {
        return entity.level().registryAccess().registry(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.getHolder(KEY))
                .map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, entity) > 0)
                .orElse(false);
    }

    private BloodResistance() {}
}
