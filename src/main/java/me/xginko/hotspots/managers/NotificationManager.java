package me.xginko.hotspots.managers;

import me.xginko.hotspots.Hotspots;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class NotificationManager extends Manager implements Listener {

    private final @NotNull Map<UUID, Boolean> notification_settings;

    public NotificationManager() {
        this.notification_settings = new ConcurrentHashMap<>();
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        notification_settings.clear();
    }

    public @NotNull CompletableFuture<Boolean> getNotificationsEnabled(@NotNull Player player) {
        if (notification_settings.containsKey(player.getUniqueId())) {
            return CompletableFuture.completedFuture(notification_settings.get(player.getUniqueId()));
        }

        return Hotspots.database()
                .getNotificationsEnabled(player.getUniqueId())
                .thenApply(notificationsEnabledStatus -> {
                    notification_settings.put(player.getUniqueId(), notificationsEnabledStatus); // Cache on request
                    return notificationsEnabledStatus; // Return database result
                });
    }

    public @NotNull CompletableFuture<Boolean> setNotificationsEnabled(@NotNull Player player, boolean enable) {
        notification_settings.put(player.getUniqueId(), enable);

        // Hide/Show Hotspot BossBars for desired target audience
        if (Manager.get(HotspotManager.class) != null) {
            if (enable) {
                Manager.get(HotspotManager.class).hideHotspotsFor(player);
            } else {
                Manager.get(HotspotManager.class).showHotspotsFor(player);
            }
        }

        return Hotspots.database().setNotificationsEnabled(player.getUniqueId(), enable);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerQuit(PlayerQuitEvent event) {
        notification_settings.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerKick(PlayerKickEvent event) {
        notification_settings.remove(event.getPlayer().getUniqueId());
    }
}
