package com.invasion;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class InvasionConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue AUTO_INVASION = B
            .comment("Start the invasion automatically after the delay below.")
            .define("automaticInvasion", true);
    public static final ModConfigSpec.IntValue INVASION_DAYS = B
            .comment("Minecraft days before the meteor strikes.")
            .defineInRange("invasionDelayDays", 3, 0, 10000);
    public static final ModConfigSpec.IntValue MAX_HORDE = B
            .comment("Maximum invaders alive around the portal at once.")
            .defineInRange("maxHordeSize", 12, 1, 200);
    public static final ModConfigSpec.IntValue SPAWN_INTERVAL = B
            .comment("Ticks between waves coming out of the portal.")
            .defineInRange("hordeSpawnIntervalTicks", 200, 20, 12000);
    public static final ModConfigSpec.BooleanValue HORDE_PURSUES = B
            .comment("While the invasion is active, small groups of invaders also emerge near players who are far from the portal and hunt them down.")
            .define("hordePursuesPlayers", true);
    public static final ModConfigSpec.IntValue PURSUIT_INTERVAL = B
            .comment("Ticks between pursuit groups for a player far from the portal.")
            .defineInRange("pursuitIntervalTicks", 600, 100, 24000);
    public static final ModConfigSpec.BooleanValue INVADERS_MINE = B
            .comment("Invaders dig through blocks to reach you (also needs the mobGriefing gamerule).")
            .define("invadersCanMine", true);
    public static final ModConfigSpec.IntValue MOSS_RADIUS = B
            .comment("How far moss spreads around a portal.")
            .defineInRange("mossSpreadRadius", 20, 0, 64);

    public static final ModConfigSpec SPEC = B.build();

    private InvasionConfig() {}
}
