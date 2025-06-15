package me.xginko.hotspots.commands.player.subcommands;

import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.player.HotspotCommand;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.WarmupManager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class JoinSubCmd extends BaseCommand.CooldownCommand {

    public JoinSubCmd() {
        super("join", HotspotsPermission.BYPASS_JOIN_COOLDOWN, Hotspots.config().commands_join_cooldown_millis);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 2) {
            return HotspotCommand.getHotspotTabCompletes()
                    .stream()
                    .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).contains(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!HotspotsPermission.JOIN_CMD.check(sender).toBoolean()) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", AdventureUtil.RED));
            return true;
        }

        if (isOnCommandCooldown(player)) {
            sendCommandCooldownMessage(player);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Hotspots.translation(player).join_failed_specify_name);
            return true;
        }

        if (Manager.get(HotspotManager.class) == null || Manager.get(WarmupManager.class) == null) {
            player.sendMessage(Hotspots.translation(player).plugin_busy);
            return true;
        }

        String name = BaseCommand.mergeArgs(args, 1);
        @Nullable Hotspot hotspot = Manager.get(HotspotManager.class).getActiveHotspotByName(name);

        if (hotspot == null) {
            player.sendMessage(Hotspots.translation(player).join_failed_not_found
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%name%").replacement(name).build()));
        } else {
            Manager.get(WarmupManager.class).startWarmup(player, hotspot);
            putOnCommandCooldown(player.getUniqueId());
        }

        return true;
    }
}