package me.xginko.hotspots.commands.player.subcommands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.BaseCommand.CooldownCommand;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.Enableable;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConfirmSubCmd extends CooldownCommand implements Enableable, Disableable {

    private static Cache<UUID, Runnable> pendingConfirms;

    public ConfirmSubCmd() {
        super("confirm", HotspotsPermission.BYPASS_CONFIRM_COOLDOWN, Hotspots.config().commands_confirm_cooldown_millis);
    }

    public static void prepareToConfirm(UUID uuid, Runnable confirm) {
        pendingConfirms.put(uuid, confirm);
    }

    @Override
    public void enable() {
        pendingConfirms = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(Hotspots.config().commands_confirm_timeout_millis)).build();
    }

    @Override
    public void disable() {
        if (pendingConfirms != null) {
            pendingConfirms.invalidateAll();
            pendingConfirms.cleanUp();
            pendingConfirms = null;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", AdventureUtil.RED));
            return true;
        }

        if (isOnCommandCooldown(player)) {
            sendCommandCooldownMessage(player);
            return true;
        }

        @Nullable Runnable pendingConfirm = pendingConfirms.getIfPresent(player.getUniqueId());

        if (pendingConfirm == null) {
            player.sendMessage(Hotspots.translation(player).confirm_nothing_pending);
        } else {
            CompletableFuture.runAsync(pendingConfirm).thenRun(() -> pendingConfirms.invalidate(player.getUniqueId()));
            putOnCommandCooldown(player.getUniqueId());
        }

        return true;
    }
}