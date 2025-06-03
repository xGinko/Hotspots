package me.xginko.hotspots.modules.broadcast;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.events.HotspotStartTickEvent;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.NotificationManager;
import me.xginko.hotspots.modules.Module;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public final class AnnounceHotspotStart extends Module implements Listener {

    public AnnounceHotspotStart() {
        super("broadcast.hotspot-start", true, """
                Notifies all players when a hotspot starts ticking.""");
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
    private void onHotspotStartTick(HotspotStartTickEvent event) {
        TextReplacementConfig name = TextReplacementConfig.builder()
                .matchLiteral("%hotspot_name%").replacement(event.getHotspot().getName()).build();

        NotificationManager notificationManager = Manager.get(NotificationManager.class);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            notificationManager.getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                if (enabled) player.sendMessage(Hotspots.translation(player.locale()).hotspot_started.replaceText(name));
            });
        }
    }
}
