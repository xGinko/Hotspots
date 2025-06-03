package me.xginko.hotspots.commands.player.subcommands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.PluginPermission;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.player.HotspotCommand;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EndSubCmd extends BaseCommand {

    private final @NotNull Cache<UUID, Long> command_cooldowns;

    public EndSubCmd() {
        super("end");
        this.command_cooldowns = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).build();
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 2 && sender.hasPermission(PluginPermission.END_CMD_OTHER.get())) {
            return HotspotCommand.getHotspotTabCompletes()
                    .stream()
                    .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).contains(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (sender instanceof Player player && !player.hasPermission(PluginPermission.BYPASS_END_COOLDOWN.get())) {
            Long lastUse = command_cooldowns.getIfPresent(player.getUniqueId());
            if (lastUse != null) {
                long time_since_last_use = System.currentTimeMillis() - lastUse;
                if (time_since_last_use < Hotspots.config().commands_end_cooldown_millis) {
                    player.sendMessage(Hotspots.translation(player).cmd_cooldown.replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(Util.formatDuration(Duration.ofMillis(Math.max(0L, Hotspots.config().commands_end_cooldown_millis - time_since_last_use))))
                            .build()));
                    return true;
                }
            } else {
                command_cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }

        if (!sender.hasPermission(PluginPermission.END_CMD.get()) && !sender.hasPermission(PluginPermission.END_CMD_OTHER.get()) ) {
            sender.sendMessage(Hotspots.translation(sender).cmd_no_permission);
            return true;
        }

        if (Manager.get(HotspotManager.class) == null) {
            sender.sendMessage(Hotspots.translation(sender).plugin_busy);
            return true;
        }

        UUID hotspot_uuid;

        if (args.length >= 2) {
            if (!sender.hasPermission(PluginPermission.END_CMD_OTHER.get())) return true;

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
            hotspot.deleteFromDatabase().thenRun(() -> sender.sendMessage(Hotspots.translation(sender).end_success
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%hotspot_name%").replacement(hotspot.getName()).build())));
        }

        return true;
    }
}