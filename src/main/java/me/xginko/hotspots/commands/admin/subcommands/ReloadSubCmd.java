package me.xginko.hotspots.commands.admin.subcommands;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.PluginPermission;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.utils.AdventureUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ReloadSubCmd extends BaseCommand {

    public ReloadSubCmd() {
        super("reload");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PluginPermission.RELOAD_CMD.get())) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  ʀᴇʟᴏᴀᴅɪɴɢ ʜᴏᴛsᴘᴏᴛs...", AdventureUtil.YELLOW));
        Hotspots plugin = Hotspots.getInstance();
        plugin.getServer().getAsyncScheduler().runNow(plugin, reload -> {
            if (plugin.reloadPlugin()) {
                sender.sendMessage(Component.text("  ʀᴇʟᴏᴀᴅ ᴄᴏᴍᴘʟᴇᴛᴇ.", AdventureUtil.ALACITY_MAGENTA));
            } else {
                sender.sendMessage(Component.text("Error!", AdventureUtil.RED));
            }
            sender.sendMessage(Component.empty());
        });

        return true;
    }
}