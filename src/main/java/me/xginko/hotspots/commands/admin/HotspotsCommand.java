package me.xginko.hotspots.commands.admin;

import com.google.common.collect.ImmutableList;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.PluginYMLCmd;
import me.xginko.hotspots.commands.admin.subcommands.ReloadSubCmd;
import me.xginko.hotspots.commands.admin.subcommands.VersionSubCmd;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HotspotsCommand extends PluginYMLCmd {

    private final @NotNull List<BaseCommand> subCommands;
    private final @NotNull List<String> subCommandTabCompletes;

    public HotspotsCommand() {
        super("hotspots");
        subCommands = ImmutableList.of(new ReloadSubCmd(), new VersionSubCmd());
        subCommandTabCompletes = ImmutableList.copyOf(subCommands.stream().map(BaseCommand::label).toList());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return subCommandTabCompletes.stream()
                    .filter(cmd -> cmd.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length > 1) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.onTabComplete(sender, command, alias, args);
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length >= 1) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.onCommand(sender, command, alias, args);
                }
            }
        }

        return true;
    }
}
