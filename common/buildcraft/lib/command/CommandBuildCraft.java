package buildcraft.lib.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import buildcraft.lib.BCLib;
import buildcraft.lib.script.ReloadableRegistryManager;

/** The /buildcraft command: "version", "changelog" and "reload" (1.12.2's command tree, on Brigadier). The version command
 * only shows the running version: 1.12.2 also asked BuildCraft's website for updates, which doesn't apply to this
 * build. */
public final class CommandBuildCraft {
    private CommandBuildCraft() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buildcraft")
            .then(Commands.literal("version").executes(ctx -> {
                String version = BCLib.VERSION;
                ctx.getSource().sendSuccess(() -> Component.translatable("command.buildcraft.version", version,
                    BCLib.MC_VERSION, version).withStyle(ChatFormatting.GREEN), false);
                if (version.contains("-pre")) {
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.buildcraft.version.prerelease"), false);
                }
                return 1;
            }))
            .then(Commands.literal("changelog").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("TODO: Implement this!"), false);
                return 1;
            }))
            .then(Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                ReloadableRegistryManager.DATA_PACKS.reloadAll();
                ctx.getSource().sendSuccess(() -> Component.translatable("command.buildcraft.reload"), true);
                return 1;
            })));
    }
}
