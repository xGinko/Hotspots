package me.xginko.hotspots.modules.extras;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DeathEffectsInsideHotspot extends Module implements Listener {

    private static NamespacedKey noDmgKey;

    private final List<FireworkEffect> firework_effects;
    private final long firework_linger_millis, firework_spawn_period;

    public DeathEffectsInsideHotspot() {
        super("extras.firework-deaths", true, """
                Spawns firework explosions if a player has died inside a hotspot radius.""");
        this.firework_linger_millis = config.getLong(configPath + ".linger-millis", 3000L);
        this.firework_spawn_period = config.getLong(configPath + ".tick-period", 4L);
        final List<String> defaults = List.of("#FFAE03", "#FE4E00", "#1A090D", "#A42CD6", "#A3EB1E");
        final List<Color> colors = config.getList(configPath + ".colors", defaults,
                "You need to configure at least 1 color.")
                .stream()
                .map(hexString -> {
                    try {
                        return Util.colorFromHexString(hexString);
                    } catch (NumberFormatException e) {
                        warn("Could not parse color '" + hexString + "'. Is it formatted correctly?");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (colors.isEmpty()) {
            warn("Parsed colors were empty. Using default colors.");
            colors.addAll(defaults.stream().map(Util::colorFromHexString).collect(Collectors.toSet()));
        }
        final boolean flicker = config.getBoolean(configPath + ".flicker", false);
        final boolean trail = config.getBoolean(configPath + ".trail", false);
        final List<FireworkEffect.Type> effectTypes = config.getList(configPath + ".types",
                Arrays.stream(FireworkEffect.Type.values()).map(Enum::name).sorted().collect(Collectors.toList()),
                "FireworkEffect Types you wish to use. Has to be a valid enum from:\n" +
                        "https://jd.papermc.io/paper/1.20/org/bukkit/FireworkEffect.Type.html")
                .stream()
                .map(configuredType -> {
                    try {
                        return FireworkEffect.Type.valueOf(configuredType);
                    } catch (IllegalArgumentException e) {
                        warn("FireworkEffect Type '" + configuredType + "' not recognized. Please use valid enums from: " +
                                "https://jd.papermc.io/paper/1.20/org/bukkit/FireworkEffect.Type.html");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (effectTypes.isEmpty()) {
            Hotspots.logger().warn("Parsed firework effect types are empty. Using all types.");
            effectTypes.addAll(Arrays.asList(FireworkEffect.Type.values()));
        }
        final List<FireworkEffect> parsedFireworkEffects = new ArrayList<>();
        for (FireworkEffect.Type effectType : effectTypes) {
            for (Color primaryColor : colors) {
                if (colors.size() == 1) {
                    parsedFireworkEffects.add(FireworkEffect.builder()
                            .withColor(primaryColor)
                            .with(effectType)
                            .flicker(flicker)
                            .trail(trail)
                            .build());
                    continue;
                }
                Color secondaryColor;
                do {
                    secondaryColor = Util.getRandomElement(colors);
                } while (secondaryColor.equals(primaryColor)); // Ensure we never combine the same colors
                parsedFireworkEffects.add(FireworkEffect.builder()
                        .withColor(primaryColor, secondaryColor)
                        .with(effectType)
                        .flicker(flicker)
                        .trail(trail)
                        .build());
            }
        }
        this.firework_effects = ImmutableList.copyOf(parsedFireworkEffects);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        noDmgKey = null;
    }

    @Override
    public void enable() {
        noDmgKey = new NamespacedKey(plugin, "no-dmg");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerDeath(PlayerDeathEvent event) {
        Location deathLoc = event.getEntity().getLocation();
        for (Hotspot hotspot : Manager.get(HotspotManager.class).getActiveHotspots()) {
            if (hotspot.contains(deathLoc)) {
                plugin.getServer().getRegionScheduler().runAtFixedRate(
                        plugin,
                        deathLoc,
                        new FireworkTask(deathLoc, firework_effects, firework_linger_millis), 1L, firework_spawn_period);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.FIREWORK_ROCKET // Faster operation
                && event.getDamager().getPersistentDataContainer().has(noDmgKey)) {
            event.setCancelled(true);
        }
    }

    private static class FireworkTask implements Consumer<ScheduledTask> {

        private final List<FireworkEffect> firework_effects;
        private final Location location;
        private final long endTime;

        public FireworkTask(Location location, List<FireworkEffect> effects, long lingerMillis) {
            this.location = location;
            this.firework_effects = effects;
            this.endTime = System.currentTimeMillis() + lingerMillis;
        }

        @Override
        public void accept(ScheduledTask scheduledTask) {
            if (System.currentTimeMillis() >= endTime) {
                scheduledTask.cancel();
                return;
            }

            Firework firework = location.getWorld().spawn(location, Firework.class);
            firework.getPersistentDataContainer().set(noDmgKey, PersistentDataType.BYTE, (byte) 1);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.clearEffects();
            meta.addEffect(Util.getRandomElement(firework_effects));
            firework.setFireworkMeta(meta);
            firework.setSilent(true);
            firework.detonate();
        }
    }
}
