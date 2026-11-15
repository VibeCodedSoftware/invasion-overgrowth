package com.invasion;

import com.invasion.block.InvasionPortalBlock;
import com.invasion.block.PortalCoreBlockEntity;
import com.invasion.entity.EntEntity;
import com.invasion.entity.InvaderMiner;
import com.invasion.entity.InvaderZombie;
import com.invasion.entity.MeteorEntity;
import com.invasion.entity.TempleGuardian;
import com.invasion.item.GloveCharge;
import com.invasion.item.ParticleGloveItem;
import com.invasion.network.CycleElementPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.invasion.InvasionMod.MODID;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.EntVillageFeature> ENT_VILLAGE =
            FEATURES.register("ent_village", com.invasion.world.EntVillageFeature::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.OvergrownDungeonFeature> OVERGROWN_DUNGEON =
            FEATURES.register("overgrown_dungeon", com.invasion.world.OvergrownDungeonFeature::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.GiantTreeFeature> GIANT_TREE =
            FEATURES.register("giant_tree", com.invasion.world.GiantTreeFeature::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.FleshNestFeature> FLESH_NEST =
            FEATURES.register("flesh_nest", com.invasion.world.FleshNestFeature::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.OvergrowthSurfaceFeature> OVERGROWTH_SURFACE =
            FEATURES.register("overgrowth_surface", com.invasion.world.OvergrowthSurfaceFeature::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, com.invasion.world.FleshSpireFeature> FLESH_SPIRE =
            FEATURES.register("flesh_spire", com.invasion.world.FleshSpireFeature::new);

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> BLOOD_SMOKE =
            PARTICLES.register("blood_smoke", () -> new net.minecraft.core.particles.SimpleParticleType(true));
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> FALLING_BLOOD_LEAF =
            PARTICLES.register("falling_blood_leaf", () -> new net.minecraft.core.particles.SimpleParticleType(false));

    // ---- Data components ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GloveCharge>> GLOVE_CHARGE = COMPONENTS.register("glove_charge",
            () -> DataComponentType.<GloveCharge>builder().persistent(GloveCharge.CODEC).networkSynchronized(GloveCharge.STREAM_CODEC).build());

    // ---- Blocks ----
    /** Meteor rock: breakable, drops itself. */
    public static final DeferredBlock<Block> BLOODY_COBBLESTONE = BLOCKS.register("bloody_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** Portal frame: same texture, cannot be broken in survival, immune to explosions and pistons. */
    public static final DeferredBlock<Block> PORTAL_FRAME = BLOCKS.register("bloody_portal_frame",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<InvasionPortalBlock> PORTAL = BLOCKS.register("invasion_portal",
            () -> new InvasionPortalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .noCollission().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()
                    .lightLevel(s -> 11).sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<com.invasion.block.BloodGeyserBlock> BLOOD_GEYSER = BLOCKS.register("blood_geyser",
            () -> new com.invasion.block.BloodGeyserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 5)));

    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> GIANT_OAK_SAPLING = giantSapling("giant_oak_sapling", "giant_oak");
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> GIANT_BIRCH_SAPLING = giantSapling("giant_birch_sapling", "giant_birch");
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> GIANT_AZALEA_SAPLING = giantSapling("giant_azalea_sapling", "giant_azalea");

    private static DeferredBlock<net.minecraft.world.level.block.SaplingBlock> giantSapling(String name, String tree) {
        net.minecraft.world.level.block.grower.TreeGrower grower = new net.minecraft.world.level.block.grower.TreeGrower("invasion_" + tree, java.util.Optional.empty(),
                java.util.Optional.of(net.minecraft.resources.ResourceKey.create(Registries.CONFIGURED_FEATURE, InvasionMod.id(tree))), java.util.Optional.empty());
        return BLOCKS.register(name, () -> new net.minecraft.world.level.block.SaplingBlock(grower, BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_SAPLING)));
    }

    // ---- Entities ----
    public static final DeferredHolder<EntityType<?>, EntityType<InvaderZombie>> INVADER = ENTITIES.register("invader",
            () -> EntityType.Builder.<InvaderZombie>of(InvaderZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("invader"));
    public static final DeferredHolder<EntityType<?>, EntityType<InvaderMiner>> INVADER_MINER = ENTITIES.register("invader_miner",
            () -> EntityType.Builder.<InvaderMiner>of(InvaderMiner::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("invader_miner"));
    public static final DeferredHolder<EntityType<?>, EntityType<TempleGuardian>> GUARDIAN = ENTITIES.register("temple_guardian",
            () -> EntityType.Builder.<TempleGuardian>of(TempleGuardian::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).fireImmune().clientTrackingRange(10).build("temple_guardian"));
    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F).fireImmune().clientTrackingRange(16).updateInterval(1).build("meteor"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntEntity>> ENT = ENTITIES.register("ent",
            () -> EntityType.Builder.<EntEntity>of(EntEntity::new, MobCategory.CREATURE)
                    .sized(1.4F, 4.4F).eyeHeight(4.0F).clientTrackingRange(10).build("ent"));

    // ---- Block entities ----
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalCoreBlockEntity>> PORTAL_CORE = BLOCK_ENTITIES.register("portal_core",
            () -> BlockEntityType.Builder.of(PortalCoreBlockEntity::new, PORTAL.get()).build(null));

    // ---- Items ----
    public static final DeferredItem<BlockItem> BLOODY_COBBLESTONE_ITEM = ITEMS.registerSimpleBlockItem(BLOODY_COBBLESTONE);
    public static final DeferredItem<BlockItem> PORTAL_FRAME_ITEM = ITEMS.registerSimpleBlockItem(PORTAL_FRAME);
    public static final DeferredItem<BlockItem> BLOOD_GEYSER_ITEM = ITEMS.registerSimpleBlockItem(BLOOD_GEYSER);
    public static final DeferredItem<BlockItem> GIANT_OAK_SAPLING_ITEM = ITEMS.register("giant_oak_sapling", () -> new com.invasion.item.GiantSaplingItem(GIANT_OAK_SAPLING.get()));
    public static final DeferredItem<BlockItem> GIANT_BIRCH_SAPLING_ITEM = ITEMS.register("giant_birch_sapling", () -> new com.invasion.item.GiantSaplingItem(GIANT_BIRCH_SAPLING.get()));
    public static final DeferredItem<BlockItem> GIANT_AZALEA_SAPLING_ITEM = ITEMS.register("giant_azalea_sapling", () -> new com.invasion.item.GiantSaplingItem(GIANT_AZALEA_SAPLING.get()));
    public static final DeferredItem<ParticleGloveItem> PARTICLE_GLOVE = ITEMS.register("particle_glove",
            () -> new ParticleGloveItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<DeferredSpawnEggItem> INVADER_EGG = ITEMS.register("invader_spawn_egg",
            () -> new DeferredSpawnEggItem(INVADER, 0x5A1414, 0x6AA834, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MINER_EGG = ITEMS.register("invader_miner_spawn_egg",
            () -> new DeferredSpawnEggItem(INVADER_MINER, 0x5B3A1E, 0xE0A828, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GUARDIAN_EGG = ITEMS.register("temple_guardian_spawn_egg",
            () -> new DeferredSpawnEggItem(GUARDIAN, 0x7A7F72, 0xE0B030, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ENT_EGG = ITEMS.register("ent_spawn_egg",
            () -> new DeferredSpawnEggItem(ENT, 0x5A3F22, 0x3A6523, new Item.Properties()));

    // ---- Sounds (mapped to vanilla sound files in sounds.json, so no third-party audio ships) ----
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOVE_FIRE = sound("glove_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOVE_REDSTONE = sound("glove_redstone");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOVE_WATER = sound("glove_water");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOVE_CHARGE_SOUND = sound("glove_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCKWAVE = sound("shockwave");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.invasion"))
                    .icon(() -> new ItemStack(PARTICLE_GLOVE.get()))
                    .displayItems((params, out) -> {
                        out.accept(BLOODY_COBBLESTONE_ITEM.get());
                        out.accept(PORTAL_FRAME_ITEM.get());
                        out.accept(BLOOD_GEYSER_ITEM.get());
                        out.accept(GIANT_OAK_SAPLING_ITEM.get());
                        out.accept(GIANT_BIRCH_SAPLING_ITEM.get());
                        out.accept(GIANT_AZALEA_SAPLING_ITEM.get());
                        out.accept(PARTICLE_GLOVE.get());
                        out.accept(ModFluids.BLOOD_BUCKET.get());
                        ItemStack full = new ItemStack(PARTICLE_GLOVE.get());
                        full.set(GLOVE_CHARGE.get(), new GloveCharge(GloveCharge.MAX, GloveCharge.MAX, GloveCharge.MAX, 0));
                        out.accept(full);
                        out.accept(INVADER_EGG.get());
                        out.accept(MINER_EGG.get());
                        out.accept(GUARDIAN_EGG.get());
                        out.accept(ENT_EGG.get());
                        out.accept(ModFlesh.FLESHLING_SPAWN_EGG.get());
                        out.accept(ModFlesh.FLESH_THING_SPAWN_EGG.get());
                        out.accept(ModFlesh.FLESHLING_EGG.get());
                        for (var b : java.util.List.of(ModFlesh.FLESH_ITEM, ModFlesh.FLESH_FOLDS_ITEM, ModFlesh.DARK_FLESH_ITEM, ModFlesh.SPOTTED_FLESH_ITEM, ModFlesh.FLESH_MAW_ITEM, ModFlesh.FLESH_HAIR_ITEM, ModFlesh.LONG_FLESH_HAIR_ITEM)) out.accept(b.get());
                        for (var b : java.util.List.of(ModWood.LOG_ITEM, ModWood.WOOD_ITEM, ModWood.STRIPPED_LOG_ITEM, ModWood.STRIPPED_WOOD_ITEM, ModWood.PLANKS_ITEM, ModWood.STAIRS_ITEM, ModWood.SLAB_ITEM, ModWood.FENCE_ITEM, ModWood.FENCE_GATE_ITEM, ModWood.TRAPDOOR_ITEM, ModWood.BUTTON_ITEM, ModWood.PRESSURE_PLATE_ITEM, ModWood.LEAVES_ITEM, ModWood.LEAF_LITTER_ITEM, ModWood.SAPLING_ITEM)) out.accept(b.get());
                        out.accept(ModWood.DOOR_ITEM.get());
                        out.accept(ModWood.BOAT.get());
                        out.accept(ModWood.CHEST_BOAT.get());
                    }).build());

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(InvasionMod.id(name)));
    }

    public static void register(IEventBus bus) {
        ModWood.init();
        ModFlesh.init();
        COMPONENTS.register(bus);
        bus.addListener(ModRegistry::onCommonSetup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModRegistry::onToolModify);
        ModFluids.register(bus);
        PARTICLES.register(bus);
        FEATURES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
        bus.addListener(ModRegistry::onAttributes);
        bus.addListener(ModRegistry::onSpawnPlacements);
        bus.addListener(ModRegistry::onPayloads);
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(INVADER.get(), InvaderZombie.createInvaderAttributes().build());
        e.put(INVADER_MINER.get(), InvaderZombie.createInvaderAttributes().build());
        e.put(GUARDIAN.get(), TempleGuardian.createGuardianAttributes().build());
        e.put(ENT.get(), EntEntity.createEntAttributes().build());
        e.put(ModFlesh.FLESHLING.get(), com.invasion.entity.FleshlingEntity.createFleshlingAttributes().build());
        e.put(ModFlesh.FLESH_THING.get(), com.invasion.entity.FleshThingEntity.createFleshThingAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent e) {
        e.register(INVADER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(INVADER_MINER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(ENT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntEntity::checkEntSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(ModFlesh.FLESHLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onPayloads(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(CycleElementPayload.TYPE, CycleElementPayload.STREAM_CODEC, CycleElementPayload::handle);
    }

    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            var fire = (net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE;
            for (var b : java.util.List.of(ModWood.LOG, ModWood.WOOD, ModWood.STRIPPED_LOG, ModWood.STRIPPED_WOOD)) fire.setFlammable(b.get(), 5, 5);
            for (var b : java.util.List.of(ModWood.PLANKS, ModWood.STAIRS, ModWood.SLAB, ModWood.FENCE, ModWood.FENCE_GATE)) fire.setFlammable(b.get(), 5, 20);
            fire.setFlammable(ModWood.LEAVES.get(), 30, 60);
            fire.setFlammable(ModWood.LEAF_LITTER.get(), 60, 100);
        });
    }

    /** Axes strip Blood Oak logs and wood. */
    private static void onToolModify(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent e) {
        if (e.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) return;
        var s = e.getState();
        net.minecraft.world.level.block.Block to = s.is(ModWood.LOG.get()) ? ModWood.STRIPPED_LOG.get() : s.is(ModWood.WOOD.get()) ? ModWood.STRIPPED_WOOD.get() : null;
        if (to != null) e.setFinalState(to.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, s.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)));
    }

    private ModRegistry() {}
}
