package me.xginko.hotspots.modules.broadcast;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.events.HotspotStopTickEvent;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.NotificationManager;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.time.Duration;

public final class AnnounceHotspotEnd extends Module implements Listener {

    public AnnounceHotspotEnd() {
        super("broadcast.hotspot-end", true, """
                Notifies all players when a hotspot stops ticking.""");
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onHotspotStopTick(HotspotStopTickEvent event) {
        final TextReplacementConfig name = TextReplacementConfig.builder()
                .matchLiteral("%hotspot_name%").replacement(event.getHotspot().getName()).build();

        NotificationManager notificationManager = Manager.get(NotificationManager.class);

        switch (event.getReason()) {
            case EXPIRED -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                        if (enabled) player.sendMessage(Hotspots.translation(player).hotspot_ended_expired
                                .replaceText(name));
                    });
                }
            }

            case CANCELLED -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                        if (enabled) player.sendMessage(Hotspots.translation(player).hotspot_ended_cancelled
                                .replaceText(name));
                    });
                }
            }

            case PLAYER_DEATH -> {
                final TextReplacementConfig owner = TextReplacementConfig.builder().matchLiteral("%hotspot_owner%")
                        .replacement(event.getHotspot().getOwnerName()).build();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                        if (enabled) player.sendMessage(Hotspots.translation(player).hotspot_ended_death
                                .replaceText(name)
                                .replaceText(owner));
                    });
                }
            }

            case PLAYER_LEAVE_TIMEOUT -> {
                final TextReplacementConfig time = TextReplacementConfig.builder().matchLiteral("%time%")
                        .replacement(Util.formatDuration(Duration.ofSeconds(Hotspots.config().disconnect_timeout_seconds))).build();
                final TextReplacementConfig owner = TextReplacementConfig.builder().matchLiteral("%hotspot_owner%")
                        .replacement(event.getHotspot().getOwnerName()).build();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                        if (enabled) player.sendMessage(Hotspots.translation(player).hotspot_timeout
                                .replaceText(name)
                                .replaceText(owner)
                                .replaceText(time));
                    });
                }
            }
        }
    }
}
