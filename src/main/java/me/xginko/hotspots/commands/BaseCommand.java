package me.xginko.hotspots.commands;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import me.xginko.hotspots.utils.Lazy;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    private final String label;

    public BaseCommand(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static @NotNull String mergeArgs(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    public static class VoidCommand extends BaseCommand {

        public VoidCommand(String label) {
            super(label);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            return Collections.emptyList();
        }
    }

    public abstract static class CooldownCommand extends BaseCommand {

        private final Lazy<Map<UUID, Long>> lastUseTime = Lazy.of(HashMap::new);
        private final HotspotsPermission bypassPermission;
        private final long cooldownMilliseconds;

        public CooldownCommand(String label, HotspotsPermission bypassPermission, long cooldownMilliseconds) {
            super(label);
            this.bypassPermission = bypassPermission;
            this.cooldownMilliseconds = cooldownMilliseconds;
        }

        public boolean hasUsedCommandBefore(UUID uuid) {
            return lastUseTime.get().containsKey(uuid);
        }

        public long getMillisSinceCommandLastUsed(UUID uuid) {
            return System.currentTimeMillis() - lastUseTime.get().getOrDefault(uuid, -1L);
        }

        public void putOnCommandCooldown(UUID uuid) {
            lastUseTime.get().put(uuid, System.currentTimeMillis());
        }

        public boolean isOnCommandCooldown(Player player) {
            return      !bypassPermission.check(player).toBoolean()
                    &&  hasUsedCommandBefore(player.getUniqueId())
                    &&  getMillisSinceCommandLastUsed(player.getUniqueId()) < cooldownMilliseconds;
        }

        public void sendCommandCooldownMessage(Player player) {
            player.sendMessage(Hotspots.translation(player).cmd_cooldown
                    .replaceText(
                            TextReplacementConfig.builder()
                                    .matchLiteral("%time%")
                                    .replacement(Util.formatDuration(Duration.ofMillis(Math.max(0L,
                                            cooldownMilliseconds - getMillisSinceCommandLastUsed(player.getUniqueId())
                                    ))))
                                    .build()
                    ));
        }
    }
}
