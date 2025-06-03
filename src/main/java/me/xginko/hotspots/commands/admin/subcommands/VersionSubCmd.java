package me.xginko.hotspots.commands.admin.subcommands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.PluginPermission;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.utils.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class VersionSubCmd extends BaseCommand {

    public VersionSubCmd() {
        super("version");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PluginPermission.VERSION_CMD.get())) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        final PluginMeta pluginYML = Hotspots.getInstance().getPluginMeta();

        sender.sendMessage(Component.newline()
                .append(Component.text("  ʜᴏᴛsᴘᴏᴛs " + pluginYML.getVersion(), AdventureUtil.ALACITY_MAGENTA)
                        .clickEvent(ClickEvent.openUrl(pluginYML.getWebsite())))
                .append(Component.text(" ʙʏ ", NamedTextColor.DARK_GRAY))
                .append(Component.text("xGɪɴᴋᴏ", AdventureUtil.GINKO_BLUE)
                        .clickEvent(ClickEvent.openUrl("https://github.com/xGinko")))
                .append(Component.newline())
        );

        return true;
    }
}