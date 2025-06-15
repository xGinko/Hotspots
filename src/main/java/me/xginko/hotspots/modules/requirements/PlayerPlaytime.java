package me.xginko.hotspots.modules.requirements;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import me.xginko.hotspots.events.player.PlayerHotspotConfirmEvent;
import me.xginko.hotspots.events.player.PlayerHotspotCreateEvent;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class PlayerPlaytime extends Module implements Listener {

    private final long minimum_playtime_minutes;

    public PlayerPlaytime() {
        super("requirements.minimum-playtime", false);
        this.minimum_playtime_minutes = config.getLong(configPath + ".playtime-minutes", 20160L, """
                20160 minutes = 14 days. Enough to get a rough set together in non-dupe survival.""");
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerCreateHotspot(PlayerHotspotCreateEvent event) {
        checkPlaytime(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerConfirmHotspot(PlayerHotspotConfirmEvent event) {
        checkPlaytime(event);
    }

    private void checkPlaytime(PlayerHotspotCreateEvent event) {
        final long playtimeMinutes = TimeUnit.MILLISECONDS.toMinutes(
                // Statistic.PLAY_ONE_MINUTE actually returns the playtime in ticks :confused:
                event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L);

        if (playtimeMinutes >= minimum_playtime_minutes
                || HotspotsPermission.BYPASS_PLAYTIME_REQUIREMENT.test(event.getPlayer()).toBoolean()) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_playtime
                .replaceText(TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(Util.formatDuration(Duration.ofMinutes(minimum_playtime_minutes - playtimeMinutes)))
                        .build()));
    }
}
