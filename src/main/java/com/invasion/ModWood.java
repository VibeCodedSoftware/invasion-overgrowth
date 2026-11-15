package com.invasion;

import com.invasion.block.BloodLeavesBlock;
import com.invasion.block.LeafLitterBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Optional;
import java.util.function.Supplier;

import static com.invasion.ModRegistry.BLOCKS;
import static com.invasion.ModRegistry.ITEMS;

/** The Blood Oak wood set. */
public final class ModWood {
    public static final BlockSetType BLOOD_OAK_SET = BlockSetType.register(new BlockSetType(InvasionMod.MODID + ":blood_oak"));
    public static final WoodType BLOOD_OAK_TYPE = WoodType.register(new WoodType(InvasionMod.MODID + ":blood_oak", BLOOD_OAK_SET));

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.CRIMSON_STEM);
    }

    public static final DeferredBlock<RotatedPillarBlock> LOG = BLOCKS.register("blood_oak_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> WOOD = BLOCKS.register("blood_oak_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_LOG = BLOCKS.register("stripped_blood_oak_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WOOD = BLOCKS.register("stripped_blood_oak_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<Block> PLANKS = BLOCKS.register("blood_oak_planks", () -> new Block(wood()));
    public static final DeferredBlock<StairBlock> STAIRS = BLOCKS.register("blood_oak_stairs", () -> new StairBlock(PLANKS.get().defaultBlockState(), wood()));
    public static final DeferredBlock<SlabBlock> SLAB = BLOCKS.register("blood_oak_slab", () -> new SlabBlock(wood()));
    public static final DeferredBlock<FenceBlock> FENCE = BLOCKS.register("blood_oak_fence", () -> new FenceBlock(wood()));
    public static final DeferredBlock<FenceGateBlock> FENCE_GATE = BLOCKS.register("blood_oak_fence_gate", () -> new FenceGateBlock(BLOOD_OAK_TYPE, wood()));
    public static final DeferredBlock<DoorBlock> DOOR = BLOCKS.register("blood_oak_door", () -> new DoorBlock(BLOOD_OAK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<TrapDoorBlock> TRAPDOOR = BLOCKS.register("blood_oak_trapdoor", () -> new TrapDoorBlock(BLOOD_OAK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<ButtonBlock> BUTTON = BLOCKS.register("blood_oak_button", () -> new ButtonBlock(BLOOD_OAK_SET, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<PressurePlateBlock> PRESSURE_PLATE = BLOCKS.register("blood_oak_pressure_plate", () -> new PressurePlateBlock(BLOOD_OAK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<BloodLeavesBlock> LEAVES = BLOCKS.register("blood_oak_leaves", () -> new BloodLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_PINK)));
    public static final DeferredBlock<LeafLitterBlock> LEAF_LITTER = BLOCKS.register("blood_leaf_litter", () -> new LeafLitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS).mapColor(MapColor.COLOR_PINK)));
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.register("blood_oak_sapling", () -> new SaplingBlock(
            new TreeGrower("invasion_blood_oak", Optional.empty(),
                    Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, InvasionMod.id("blood_oak"))), Optional.empty()),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    // ---- items ----
    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerSimpleBlockItem(LOG);
    public static final DeferredItem<BlockItem> WOOD_ITEM = ITEMS.registerSimpleBlockItem(WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_LOG_ITEM = ITEMS.registerSimpleBlockItem(STRIPPED_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_WOOD_ITEM = ITEMS.registerSimpleBlockItem(STRIPPED_WOOD);
    public static final DeferredItem<BlockItem> PLANKS_ITEM = ITEMS.registerSimpleBlockItem(PLANKS);
    public static final DeferredItem<BlockItem> STAIRS_ITEM = ITEMS.registerSimpleBlockItem(STAIRS);
    public static final DeferredItem<BlockItem> SLAB_ITEM = ITEMS.registerSimpleBlockItem(SLAB);
    public static final DeferredItem<BlockItem> FENCE_ITEM = ITEMS.registerSimpleBlockItem(FENCE);
    public static final DeferredItem<BlockItem> FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem(FENCE_GATE);
    public static final DeferredItem<DoubleHighBlockItem> DOOR_ITEM = ITEMS.register("blood_oak_door", () -> new DoubleHighBlockItem(DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(TRAPDOOR);
    public static final DeferredItem<BlockItem> BUTTON_ITEM = ITEMS.registerSimpleBlockItem(BUTTON);
    public static final DeferredItem<BlockItem> PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> LEAVES_ITEM = ITEMS.registerSimpleBlockItem(LEAVES);
    public static final DeferredItem<BlockItem> LEAF_LITTER_ITEM = ITEMS.registerSimpleBlockItem(LEAF_LITTER);
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerSimpleBlockItem(SAPLING);

    // ---- boats (Boat.Type extended through META-INF/enumextensions.json) ----
    @SuppressWarnings("unused")
    public static final EnumProxy<Boat.Type> BLOOD_OAK_BOAT_TYPE = new EnumProxy<>(Boat.Type.class,
            (Supplier<Block>) () -> PLANKS.get(), InvasionMod.MODID + ":blood_oak",
            (Supplier<Item>) () -> ModWood.BOAT.get(), (Supplier<Item>) () -> ModWood.CHEST_BOAT.get(),
            (Supplier<Item>) () -> Items.STICK, false);
    public static final DeferredItem<BoatItem> BOAT = ITEMS.register("blood_oak_boat",
            () -> new BoatItem(false, BLOOD_OAK_BOAT_TYPE.getValue(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BoatItem> CHEST_BOAT = ITEMS.register("blood_oak_chest_boat",
            () -> new BoatItem(true, BLOOD_OAK_BOAT_TYPE.getValue(), new Item.Properties().stacksTo(1)));

    public static void init() {}

    private ModWood() {}
}
