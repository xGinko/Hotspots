package me.xginko.hotspots.commands.player.subcommands;

import com.google.common.collect.ImmutableList;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.NotificationManager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class NotifsSubCmd extends BaseCommand.CooldownCommand {

    private final @NotNull List<String> tabCompletes;

    public NotifsSubCmd() {
        super("notifs", HotspotsPermission.BYPASS_NOTIFS_COOLDOWN, Hotspots.config().commands_notifs_cooldown_millis);
        this.tabCompletes = ImmutableList.of("off", "on", "hide", "show", "disable", "enable", "disabled", "enabled");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return args.length == 2 && HotspotsPermission.NOTIFS_CMD.check(sender).toBoolean() ? tabCompletes : Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", AdventureUtil.RED));
            return true;
        }

        if (!HotspotsPermission.NOTIFS_CMD.check(player).toBoolean()) {
            player.sendMessage(Hotspots.translation(player).cmd_no_permission);
            return true;
        }

        if (isOnCommandCooldown(player)) {
            sendCommandCooldownMessage(player);
            return true;
        }

        NotificationManager notificationManager = Manager.get(NotificationManager.class);

        if (notificationManager == null) {
            player.sendMessage(Hotspots.translation(player).plugin_busy);
            return true;
        }

        if (args.length == 1) {
            // Player only entered "/hotspot notifs" so we treat it like a toggle
            notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(notifsEnabled -> {
                notificationManager.setNotificationsEnabled(player.getUniqueId(), !notifsEnabled).thenRun(() -> {
                    if (notifsEnabled) player.sendMessage(Hotspots.translation(player).notifs_disabled);
                    else player.sendMessage(Hotspots.translation(player).notifs_enabled);
                });
            });
            putOnCommandCooldown(player.getUniqueId());
            return true;
        }

        if (args.length == 2) {
            switch (args[1].toLowerCase()) {
                case "on", "show", "enable", "enabled" -> {
                    notificationManager.setNotificationsEnabled(player.getUniqueId(), true)
                            .thenRun(() -> player.sendMessage(Hotspots.translation(player).notifs_enabled));
                }
                case "off", "hide", "disable", "disabled" -> {
                    notificationManager.setNotificationsEnabled(player.getUniqueId(), false)
                            .thenRun(() -> player.sendMessage(Hotspots.translation(player).notifs_disabled));
                }
            }
            putOnCommandCooldown(player.getUniqueId());
        }

        return true;
    }
}