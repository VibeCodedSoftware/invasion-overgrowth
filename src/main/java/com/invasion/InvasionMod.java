package com.invasion;

import com.invasion.world.InvasionCommands;
import com.invasion.world.InvasionManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(InvasionMod.MODID)
public class InvasionMod {
    public static final String MODID = "invasion";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Level> OVERGROWTH = ResourceKey.create(Registries.DIMENSION, id("the_overgrowth"));

    public InvasionMod(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, InvasionConfig.SPEC);

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> InvasionManager.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> InvasionCommands.register(e.getDispatcher()));
        SelfTest.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
