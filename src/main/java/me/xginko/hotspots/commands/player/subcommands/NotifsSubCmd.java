package me.xginko.hotspots.commands.player.subcommands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.PluginPermission;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.NotificationManager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class NotifsSubCmd extends BaseCommand {

    private final @NotNull List<String> tabCompletes;
    private final @NotNull Cache<UUID, Long> command_cooldowns;

    public NotifsSubCmd() {
        super("notifs");
        this.tabCompletes = ImmutableList.of("off", "on", "hide", "show", "disable", "enable", "disabled", "enabled");
        this.command_cooldowns = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).build();
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return args.length == 2 && sender.hasPermission(PluginPermission.NOTIFS_CMD.get()) ? tabCompletes : Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PluginPermission.NOTIFS_CMD.get())) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", AdventureUtil.RED));
            return true;
        }

        if (!player.hasPermission(PluginPermission.BYPASS_NOTIFS_COOLDOWN.get())) {
            Long lastUse = command_cooldowns.getIfPresent(player.getUniqueId());
            if (lastUse != null) {
                long time_since_last_use = System.currentTimeMillis() - lastUse;
                if (time_since_last_use < Hotspots.config().commands_notifs_cooldown_millis) {
                    player.sendMessage(Hotspots.translation(player).cmd_cooldown.replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(Util.formatDuration(Duration.ofMillis(Math.max(0L, Hotspots.config().commands_notifs_cooldown_millis - time_since_last_use))))
                            .build()));
                    return true;
                }
            } else {
                command_cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
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
        }

        return true;
    }
}