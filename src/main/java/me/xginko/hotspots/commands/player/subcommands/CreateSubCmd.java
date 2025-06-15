package me.xginko.hotspots.commands.player.subcommands;

import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.BaseCommand.CooldownCommand;
import me.xginko.hotspots.events.player.PlayerHotspotConfirmEvent;
import me.xginko.hotspots.events.player.PlayerHotspotCreateEvent;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.BossBarUtil;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class CreateSubCmd extends CooldownCommand {

    public CreateSubCmd() {
        super("create", HotspotsPermission.BYPASS_CREATE_COOLDOWN, Hotspots.config().commands_create_cooldown_millis);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(HotspotsPermission.CREATE_CMD.get())) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return Hotspots.config().bossbar_colors.stream()
                    .map(Enum::name)
                    .filter(cmd -> cmd.startsWith(args[1]))
                    .sorted()
                    .toList();
        }

        if (args.length == 3) {
            return Hotspots.config().bossbar_name_suggestions.stream()
                    .map(suggestion -> suggestion.replace("%player%", sender.getName()))
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(HotspotsPermission.CREATE_CMD.get())) {
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

        @Nullable HotspotManager hotspotManager = Manager.get(HotspotManager.class);
        if (hotspotManager == null) {
            player.sendMessage(Hotspots.translation(player).plugin_busy);
            return true;
        }

        @Nullable Hotspot active = hotspotManager.getActiveHotspotByUUID(player.getUniqueId());
        if (active != null) {
            player.sendMessage(Hotspots.translation(player).create_failed_already_running);
            return true;
        }

        Hotspot hotspot;

        if (args.length == 1) { // Player has just entered "/hotspot create", therefore we use defaults
            hotspot = new Hotspot(player, BossBarUtil.getBossBar(player.displayName()));
        } else {
            // Get BossBar color
            final BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(args[1]);
                if (!Hotspots.config().bossbar_colors.contains(color))
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                player.sendMessage(Hotspots.translation(player).create_failed_color
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%color_arg%").replacement(args[1]).build()));
                return true;
            }

            // Get BossBar name
            if (args.length == 2) { // Player has only specified a BossBar Color, ex. "/hotspot create PINK"
                hotspot = new Hotspot(player, BossBarUtil.getBossBar(player.displayName(), color));
            } else { // Player has specified a BossBar Color and name, ex. "/hotspot create PINK <rainbow>love u"
                String nameArg = BaseCommand.mergeArgs(args, 2).replace("%player%", player.getName());
                try {
                    hotspot = new Hotspot(player, BossBarUtil.getBossBar(nameArg, color));
                    if (hotspot.getName().length() > Hotspots.config().bossbar_title_maxlength) {
                        player.sendMessage(Hotspots.translation(player).create_failed_name_too_long);
                        return true;
                    }
                } catch (Throwable t) {
                    Hotspots.logger().warn("Player {} tried to create hotspot that threw an exception. Name: {}", player.getName(), nameArg);
                    return true;
                }
            }
        }

        PlayerHotspotCreateEvent hotspotCreateEvent = new PlayerHotspotCreateEvent(
                false,
                hotspot,
                player,
                PlayerHotspotCreateEvent.Reason.COMMAND,
                false
        );

        if (!hotspotCreateEvent.callEvent()) {
            return true;
        }

        // Show confirm dialog message
        TextReplacementConfig hotspot_title = TextReplacementConfig.builder()
                .matchLiteral("%hotspot_title%")
                .replacement(hotspot.getBossBar().name())
                .build();
        Hotspots.translation(player).create_confirm.forEach(line -> sender.sendMessage(line.replaceText(hotspot_title)));

        // Add runnable for hotspot creation to confirm cmd
        ConfirmSubCmd.prepareToConfirm(player.getUniqueId(), () -> {
            PlayerHotspotConfirmEvent hotspotConfirmEvent = new PlayerHotspotConfirmEvent(
                    hotspot,
                    player,
                    false
            );

            if (!hotspotConfirmEvent.callEvent()) {
                return;
            }

            putOnCommandCooldown(player.getUniqueId());

            // Add hotspot to queue, leaving the manager to pull it into ticking when the time is right
            hotspotManager.getQueuedHotspots().add(hotspotConfirmEvent.getHotspot());

            player.sendMessage(Hotspots.translation(player).create_success.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("%hotspot_name%").replacement(hotspotConfirmEvent.getHotspot().getName()).build()));
        });

        return true;
    }
}