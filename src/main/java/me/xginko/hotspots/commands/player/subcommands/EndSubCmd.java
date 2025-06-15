package me.xginko.hotspots.commands.player.subcommands;

import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.player.HotspotCommand;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EndSubCmd extends BaseCommand.CooldownCommand {

    public EndSubCmd() {
        super("end", HotspotsPermission.BYPASS_END_COOLDOWN, Hotspots.config().commands_end_cooldown_millis);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 2 && sender.hasPermission(HotspotsPermission.END_CMD_OTHER.get())) {
            return HotspotCommand.getHotspotTabCompletes()
                    .stream()
                    .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).contains(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!HotspotsPermission.END_CMD.check(sender).toBoolean() && !HotspotsPermission.END_CMD_OTHER.check(sender).toBoolean()) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        if (Manager.get(HotspotManager.class) == null) {
            sender.sendMessage(Hotspots.translation(sender).plugin_busy);
            return true;
        }

        if (sender instanceof Player player && isOnCommandCooldown(player)) {
            sendCommandCooldownMessage(player);
            return true;
        }

        UUID hotspot_uuid;

        if (args.length >= 2) {
            if (!sender.hasPermission(HotspotsPermission.END_CMD_OTHER.get())) return true;

            Player target_player = Bukkit.getPlayer(args[1]);

            if (target_player == null) {
                String name = BaseCommand.mergeArgs(args, 1);
                Hotspot hotspot = Manager.get(HotspotManager.class).getActiveHotspotByName(name);
                if (hotspot == null) {
                    sender.sendMessage(Component.text("No hotspot or player found with name '" + name + "'", AdventureUtil.RED));
                } else {
                    sender.sendMessage(Component.text("Ending hotspot '" + hotspot.getName() + "'...", AdventureUtil.ALACITY_MAGENTA));
                    hotspot.end();
                    hotspot.deleteFromDatabase().thenRun(() -> sender.sendMessage(Hotspots.translation(sender).end_success
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%hotspot_name%").replacement(hotspot.getName()).build())));
                }
                return true;
            }

            hotspot_uuid = target_player.getUniqueId();
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Please specify a player or hotspot."));
                return true;
            }

            hotspot_uuid = player.getUniqueId();
        }

        Hotspot hotspot = Manager.get(HotspotManager.class).getActiveHotspotByUUID(hotspot_uuid);

        if (hotspot == null) {
            sender.sendMessage(Hotspots.translation(sender).not_hosting_hotspot);
        } else {
            hotspot.end();
            hotspot.deleteFromDatabase().thenRun(() -> {
                sender.sendMessage(Hotspots.translation(sender).end_success
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%hotspot_name%").replacement(hotspot.getName()).build()));
                if (sender instanceof Player player)
                    putOnCommandCooldown(player.getUniqueId());
            });
        }

        return true;
    }
}