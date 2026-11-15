package com.invasion.world;

import com.invasion.InvasionMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** /hw invasion start|stop|status and /hw temple. Requires cheats (permission level 2). */
public final class InvasionCommands {

    /** Registered as both /hw (the original command) and /invasion. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher, "hw");
        registerRoot(dispatcher, "invasion");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("invasion")
                        .then(Commands.literal("start")
                                .executes(c -> start(c.getSource(), c.getSource().getPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> start(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("stop").executes(c -> {
                            InvasionManager.stop(c.getSource().getServer());
                            c.getSource().sendSuccess(() -> Component.literal("Invasion stopped."), true);
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(c -> {
                            c.getSource().sendSuccess(() -> InvasionManager.status(c.getSource().getServer()), false);
                            return 1;
                        })))
                .then(Commands.literal("temple").executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    ServerLevel hostile = c.getSource().getServer().getLevel(InvasionMod.OVERGROWTH);
                    if (hostile == null) {
                        c.getSource().sendFailure(Component.literal("The Overgrowth dimension not found."));
                        return 0;
                    }
                    PortalTeleporter.sendToTemple(player, hostile, InvasionData.get(c.getSource().getServer()));
                    return 1;
                })));
    }

    private static int start(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        // Only one invasion per world, unless started by a player in creative mode.
        InvasionData data = InvasionData.get(source.getServer());
        ServerPlayer caller = source.getPlayer();
        boolean creative = caller != null && caller.isCreative();
        if (data.state() != InvasionData.State.WAITING && !creative) {
            source.sendFailure(Component.literal("This world has already had its invasion. Switch to creative mode to start another."));
            return 0;
        }
        if (InvasionManager.start(source.getServer(), player)) {
            source.sendSuccess(() -> Component.literal("Invasion started. Look up!"), true);
            return 1;
        }
        source.sendFailure(Component.literal("No players in the Overworld to target."));
        return 0;
    }

    private InvasionCommands() {}
}
