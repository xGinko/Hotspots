package me.xginko.hotspots.managers;

import me.xginko.hotspots.Hotspots;
import net.kyori.adventure.audience.Audience;
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

    public @NotNull CompletableFuture<Boolean> getNotificationsEnabled(@NotNull UUID uuid) {
        if (notification_settings.containsKey(uuid)) {
            return CompletableFuture.completedFuture(notification_settings.get(uuid));
        }

        return Hotspots.database().getNotificationsEnabled(uuid)
                .thenApply(enabled -> notification_settings.put(uuid, enabled));
    }

    public @NotNull CompletableFuture<Boolean> setNotificationsEnabled(@NotNull UUID uuid, boolean enable) {
        notification_settings.put(uuid, enable);

        // Hide/Show Hotspot BossBars for desired target audience
        Audience entity = plugin.getServer().getEntity(uuid);
        HotspotManager hotspotManager = Manager.get(HotspotManager.class);
        if (entity != null && hotspotManager != null) {
            if (enable) hotspotManager.hideHotspotsFor(entity);
            else hotspotManager.showHotspotsFor(entity);
        }

        return Hotspots.database().setNotificationsEnabled(uuid, enable);
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
