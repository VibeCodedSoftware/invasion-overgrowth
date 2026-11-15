package com.invasion;

import com.invasion.block.FleshPlantBlock;
import com.invasion.block.FleshlingEggBlock;
import com.invasion.entity.BloodGlobEntity;
import com.invasion.entity.FleshThingEntity;
import com.invasion.entity.FleshlingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.invasion.ModRegistry.BLOCKS;
import static com.invasion.ModRegistry.ENTITIES;
import static com.invasion.ModRegistry.ITEMS;

/** The Flesh: its blocks, plants, creatures and the Fleshling egg. */
public final class ModFlesh {
    private static BlockBehaviour.Properties flesh(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.8F).sound(SoundType.MUD);
    }

    public static final DeferredBlock<Block> FLESH = BLOCKS.register("flesh_block", () -> new Block(flesh(MapColor.COLOR_PINK)));
    public static final DeferredBlock<Block> FLESH_FOLDS = BLOCKS.register("flesh_folds", () -> new Block(flesh(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<Block> DARK_FLESH = BLOCKS.register("dark_flesh", () -> new Block(flesh(MapColor.COLOR_RED)));
    public static final DeferredBlock<Block> SPOTTED_FLESH = BLOCKS.register("spotted_flesh", () -> new Block(flesh(MapColor.TERRACOTTA_PINK)));
    public static final DeferredBlock<Block> FLESH_MAW = BLOCKS.register("flesh_maw", () -> new Block(flesh(MapColor.COLOR_PINK).noOcclusion()));
    public static final DeferredBlock<FleshPlantBlock> FLESH_HAIR = BLOCKS.register("flesh_hair",
            () -> new FleshPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.SNOW)));
    public static final DeferredBlock<FleshPlantBlock> LONG_FLESH_HAIR = BLOCKS.register("long_flesh_hair",
            () -> new FleshPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.SNOW)));
    public static final DeferredBlock<FleshlingEggBlock> FLESHLING_EGG_BLOCK = BLOCKS.register("fleshling_egg",
            () -> new FleshlingEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.MUD).noOcclusion().randomTicks()));

    public static final DeferredItem<BlockItem> FLESH_ITEM = ITEMS.registerSimpleBlockItem(FLESH);
    public static final DeferredItem<BlockItem> FLESH_FOLDS_ITEM = ITEMS.registerSimpleBlockItem(FLESH_FOLDS);
    public static final DeferredItem<BlockItem> DARK_FLESH_ITEM = ITEMS.registerSimpleBlockItem(DARK_FLESH);
    public static final DeferredItem<BlockItem> SPOTTED_FLESH_ITEM = ITEMS.registerSimpleBlockItem(SPOTTED_FLESH);
    public static final DeferredItem<BlockItem> FLESH_MAW_ITEM = ITEMS.registerSimpleBlockItem(FLESH_MAW);
    public static final DeferredItem<BlockItem> FLESH_HAIR_ITEM = ITEMS.registerSimpleBlockItem(FLESH_HAIR);
    public static final DeferredItem<BlockItem> LONG_FLESH_HAIR_ITEM = ITEMS.registerSimpleBlockItem(LONG_FLESH_HAIR);
    public static final DeferredItem<BlockItem> FLESHLING_EGG = ITEMS.register("fleshling_egg",
            () -> new BlockItem(FLESHLING_EGG_BLOCK.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<EntityType<?>, EntityType<FleshlingEntity>> FLESHLING = ENTITIES.register("fleshling",
            () -> EntityType.Builder.<FleshlingEntity>of(FleshlingEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.05F).clientTrackingRange(8).build("fleshling"));
    public static final DeferredHolder<EntityType<?>, EntityType<FleshThingEntity>> FLESH_THING = ENTITIES.register("flesh_thing",
            () -> EntityType.Builder.<FleshThingEntity>of(FleshThingEntity::new, MobCategory.MONSTER)
                    .sized(2.6F, 3.4F).fireImmune().clientTrackingRange(10).build("flesh_thing"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodGlobEntity>> BLOOD_GLOB = ENTITIES.register("blood_glob",
            () -> EntityType.Builder.<BloodGlobEntity>of(BloodGlobEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(5).build("blood_glob"));

    public static final DeferredItem<DeferredSpawnEggItem> FLESHLING_SPAWN_EGG = ITEMS.register("fleshling_spawn_egg",
            () -> new DeferredSpawnEggItem(FLESHLING, 0xE07A7A, 0x5A1414, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FLESH_THING_SPAWN_EGG = ITEMS.register("flesh_thing_spawn_egg",
            () -> new DeferredSpawnEggItem(FLESH_THING, 0xE07A7A, 0xF0F0F0, new Item.Properties()));

    public static void init() {}

    private ModFlesh() {}
}
