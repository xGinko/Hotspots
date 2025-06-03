package me.xginko.hotspots.modules.extras;

import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.events.HotspotStopTickEvent;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.modules.Module;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

public final class EndHotspotOnDeath extends Module implements Listener {

    public EndHotspotOnDeath() {
        super("extras.end-hotspot-on-death", true, """
                Whether to end the hotspot when the owner dies.""");
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
    private void onPlayerDeath(PlayerDeathEvent event) {
        @Nullable Hotspot hotspot = Manager.get(HotspotManager.class)
                .getActiveHotspotByUUID(event.getPlayer().getUniqueId());
        if (hotspot == null) return;

        HotspotStopTickEvent stopEvent = new HotspotStopTickEvent(
                event.isAsynchronous(), hotspot, HotspotStopTickEvent.Reason.PLAYER_DEATH, false);
        plugin.getServer().getPluginManager().callEvent(stopEvent);

        if (!stopEvent.isCancelled()) {
            stopEvent.getHotspot().end();
            stopEvent.getHotspot().deleteFromDatabase();
        }
    }
}
