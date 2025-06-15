package me.xginko.hotspots.modules.requirements;

import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.utils.permissions.HotspotsPermission;
import me.xginko.hotspots.events.player.PlayerHotspotConfirmEvent;
import me.xginko.hotspots.events.player.PlayerHotspotCreateEvent;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.LocationUtil;
import me.xginko.hotspots.utils.MathHelper;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldBounds extends Module implements Listener {

    private final @NotNull Map<String, HotspotBounds> hotspot_bounds = new HashMap<>(3);

    public WorldBounds() {
        super("requirements.world-bounds", true, """
                Hotspot creation limits configurable per world.\s
                Uses the names of the world folders in your server directory.""");
        String boundsPath = configPath + ".worlds";
        config.master().makeSectionLenient(boundsPath);
        config.master().addExample(boundsPath + ".world.y-limit.upper", 300);
        config.master().addExample(boundsPath + ".world.y-limit.lower", -50);
        config.master().addExample(boundsPath + ".world.spawn-distance.min-distance", 8000.0);
        config.master().addExample(boundsPath + ".world.spawn-distance.max-distance", 2999900.0);
        config.master().addExample(boundsPath + ".world_nether.y-limit.upper", 127);
        config.master().addExample(boundsPath + ".world_nether.y-limit.lower", 6);
        config.master().addExample(boundsPath + ".world.spawn-distance.min-distance", 1000.0);
        config.master().addExample(boundsPath + ".world.spawn-distance.max-distance", 2999900.0);
        config.master().addExample(boundsPath + ".world_the_end.y-limit.upper", 300);
        config.master().addExample(boundsPath + ".world_the_end.y-limit.lower", 30);
        config.master().addExample(boundsPath + ".world.spawn-distance.min-distance", 8000.0);
        config.master().addExample(boundsPath + ".world.spawn-distance.max-distance", 2999900.0);
        ConfigSection limits_section = config.master().getConfigSection(boundsPath);
        for (String world_name : limits_section.getKeys(false)) {
            this.hotspot_bounds.put(world_name, new HotspotBounds(
                    config.master().getInteger(boundsPath + "." + world_name + ".y-limit.upper", 232),
                    config.master().getInteger(boundsPath + "." + world_name + ".y-limit.lower", 16),
                    config.master().getDouble(boundsPath + "." + world_name + ".spawn-distance.min-distance", 1000.0),
                    config.master().getDouble(boundsPath + "." + world_name + ".spawn-distance.max-distance", 2999900.0)
            ));
        }
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
    private void onHotspotCreate(PlayerHotspotCreateEvent event) {
        handleHotspotCreate(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onHotspotConfirm(PlayerHotspotConfirmEvent event) {
        handleHotspotCreate(event);
    }

    private void handleHotspotCreate(PlayerHotspotCreateEvent event) {
        Location hotspotLocation = event.getHotspot().getCenterLocation();
        if (!hotspot_bounds.containsKey(hotspotLocation.getWorld().getName())) return;
        if (HotspotsPermission.BYPASS_HOTSPOT_BOUNDS.check(event.getPlayer()).toBoolean()) return;

        HotspotBounds bounds = hotspot_bounds.get(hotspotLocation.getWorld().getName());

        if (hotspotLocation.getY() > bounds.upper_y || hotspotLocation.getY() < bounds.lower_y) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_height_limit
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%upper%").replacement(bounds.upper_y.toString()).build())
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%lower%").replacement(bounds.lower_y.toString()).build()));
            return;
        }

        double spawnDistSquared = LocationUtil.getBlockDistanceTo00Squared(hotspotLocation);

        if (spawnDistSquared < bounds.spawn_min_dist_squared) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_spawn_too_close
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%min_distance%").replacement(bounds.spawn_min_dist.toString()).build()));
            return;
        }

        if (spawnDistSquared > bounds.spawn_max_dist_squared) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_spawn_too_far
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%max_distance%").replacement(bounds.spawn_max_dist.toString()).build()));
        }
    }

    // General method for other features
    public boolean isOutOfBounds(Location location) {
        if (!hotspot_bounds.containsKey(location.getWorld().getName())) {
            return false;
        }

        HotspotBounds bounds = hotspot_bounds.get(location.getWorld().getName());

        if (location.getY() > bounds.upper_y || location.getY() < bounds.lower_y) {
            return true;
        }

        double spawnDistSquared = LocationUtil.getBlockDistanceTo00Squared(location);
        return spawnDistSquared < bounds.spawn_min_dist_squared || spawnDistSquared > bounds.spawn_max_dist_squared;
    }

    private static final class HotspotBounds {

        private final @NotNull Integer upper_y, lower_y;
        private final @NotNull Double spawn_min_dist, spawn_max_dist, spawn_min_dist_squared, spawn_max_dist_squared;

        private HotspotBounds(@NotNull Integer upper_y, @NotNull Integer lower_y, @NotNull Double spawn_min_dist, @NotNull Double spawn_max_dist) {
            this.upper_y = upper_y;
            this.lower_y = lower_y;
            this.spawn_min_dist = spawn_min_dist;
            this.spawn_max_dist = spawn_max_dist;
            this.spawn_min_dist_squared = MathHelper.square(spawn_min_dist);
            this.spawn_max_dist_squared = MathHelper.square(spawn_max_dist);
        }

        @Override
        public int hashCode() {
            return Objects.hash(upper_y, lower_y, spawn_min_dist, spawn_max_dist, spawn_min_dist_squared, spawn_max_dist_squared);
        }
    }
}
