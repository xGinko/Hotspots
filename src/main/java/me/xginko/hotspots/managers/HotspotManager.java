package me.xginko.hotspots.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableSet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.events.HotspotQueueEvent;
import me.xginko.hotspots.events.HotspotStartTickEvent;
import me.xginko.hotspots.events.HotspotStopTickEvent;
import me.xginko.hotspots.events.player.PlayerHotspotConfirmEvent;
import me.xginko.hotspots.events.player.PlayerHotspotCreateEvent;
import me.xginko.hotspots.utils.LocationUtil;
import me.xginko.hotspots.utils.MathHelper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HotspotManager extends Manager implements Consumer<ScheduledTask>, Listener {

    private final @NotNull Set<Hotspot> ticking_hotspots;
    private final @NotNull SortedSet<Hotspot> queued_hotspots;
    private final @NotNull Cache<UUID, ScheduledTask> timeout_tasks;

    private @Nullable ScheduledTask tick_task;

    public HotspotManager() {
        this.ticking_hotspots = Collections.newSetFromMap(new ConcurrentHashMap<>(Hotspots.config().max_active_hotspots));
        this.queued_hotspots = new TreeSet<>(Comparator.comparingLong(Hotspot::getCreationTime));
        this.timeout_tasks = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Hotspots.config().disconnect_timeout_seconds)).build();
    }

    @Override
    public void enable() {
        Hotspots.database().getAllHotspots().thenAccept(queued_hotspots::addAll).thenRun(() -> {
            tick_task = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                    plugin,
                    this,
                    1L,
                    Hotspots.config().hotspot_tick_period_millis,
                    TimeUnit.MILLISECONDS);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        });
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (tick_task != null) {
            tick_task.cancel();
            tick_task = null;
        }
        for (Iterator<Hotspot> iterator : iterators()) {
            while (iterator.hasNext()) {
                Hotspot hotspot = iterator.next();
                hotspot.end();
                ticking_hotspots.remove(hotspot);
                queued_hotspots.remove(hotspot);
                try {
                    if (!hotspot.hasExpired()) {
                        // Wait for save because we are most likely also reloading db
                        hotspot.saveToDatabase().get(3, TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    Hotspots.logger().error("Error saving hotspot '{}' during disable!", hotspot.getName(), e);
                }
            }
        }
    }

    @Override
    public void accept(ScheduledTask task) {
        while (!queued_hotspots.isEmpty() && ticking_hotspots.size() < Hotspots.config().max_active_hotspots) {
            // Get the hotspot that has been waiting the longest first
            Hotspot greatest_wait_time = queued_hotspots.first();

            // Call start event as cancelled when creator isn't online on init (Ex. Hotspot was queued but player left in meantime)
            Player hotspotOwner = plugin.getServer().getPlayer(greatest_wait_time.getOwnerUUID());
            HotspotStartTickEvent startEvent = new HotspotStartTickEvent(
                    greatest_wait_time, hotspotOwner == null || !hotspotOwner.isOnline());
            plugin.getServer().getPluginManager().callEvent(startEvent);

            // If event was not cancelled, start ticking hotspot
            if (!startEvent.isCancelled()) {
                ticking_hotspots.add(startEvent.getHotspot());
            }

            // Remove hotspot from queue regardless of action
            queued_hotspots.remove(startEvent.getHotspot());
        }

        for (Hotspot hotspot : ticking_hotspots) {
            try {
                // Tick hotspot, updating BossBar progression and visibility for online players
                hotspot.tick();

                // Check if hotspot should no longer be ticked
                if (hotspot.hasExpired() || hotspot.hasEnded()) {
                    HotspotStopTickEvent stopEvent = new HotspotStopTickEvent(
                            hotspot,
                            hotspot.hasExpired() ? HotspotStopTickEvent.Reason.EXPIRED : HotspotStopTickEvent.Reason.CANCELLED,
                            false);
                    plugin.getServer().getPluginManager().callEvent(stopEvent);
                    if (!stopEvent.isCancelled()) {
                        stopEvent.getHotspot().end(); // Calling end hides BossBar
                        ticking_hotspots.remove(stopEvent.getHotspot());
                    }
                }
            } catch (Throwable t) {
                Hotspots.logger().warn("Encountered an error while ticking hotspot for player '{}'! - {}",
                        hotspot.getOwnerName(), t.getLocalizedMessage());
                hotspot.end();
                ticking_hotspots.remove(hotspot);
                plugin.onException();
            }
        }
    }

    public @NotNull Set<Hotspot> getActiveHotspots() {
        return ticking_hotspots;
    }

    public @NotNull SortedSet<Hotspot> getQueuedHotspots() {
        return queued_hotspots;
    }

    public void showHotspotsFor(@NotNull Audience audience) {
        for (Hotspot hotspot : ticking_hotspots) {
            audience.showBossBar(hotspot.getBossBar());
        }
    }

    public void hideHotspotsFor(@NotNull Audience audience) {
        for (Hotspot hotspot : ticking_hotspots) {
            audience.hideBossBar(hotspot.getBossBar());
        }
    }

    @UnmodifiableView
    public @NotNull Set<Iterator<Hotspot>> iterators() {
        return ImmutableSet.of(ticking_hotspots.iterator(), queued_hotspots.iterator());
    }

    public @Nullable Hotspot getActiveHotspotByUUID(@NotNull UUID uuid) {
        for (Iterator<Hotspot> iterator : iterators()) {
            while (iterator.hasNext()) {
                Hotspot hotspot = iterator.next();
                // Compare most significant bits first as this makes it faster if it's not a match
                if (hotspot.getOwnerUUID().getMostSignificantBits() == uuid.getMostSignificantBits()
                        && hotspot.getOwnerUUID().getLeastSignificantBits() == uuid.getLeastSignificantBits()) {
                    return hotspot;
                }
            }
        }
        return null;
    }

    public @Nullable Hotspot getActiveHotspotByName(@NotNull String name) {
        for (Iterator<Hotspot> hotspotIterator : iterators()) {
            while (hotspotIterator.hasNext()) {
                Hotspot hotspot = hotspotIterator.next();
                // Allow matching playername as well as hotspot name in case players include unicode symbols
                if (hotspot.getName().equalsIgnoreCase(name) || hotspot.getOwnerName().equalsIgnoreCase(name)) {
                    return hotspot;
                }
            }
        }
        return null;
    }

    /*
     *   Timeout logic for players that leave while their hotspot is still running
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent event) {
        @NotNull Player player = event.getPlayer();

        // Cancel any previous timeout task if present
        stopTimeoutIfHasHotspot(player);

        // Do not look in database if player already has a hotspot queued or ticking
        @Nullable Hotspot activeHotspot = getActiveHotspotByUUID(player.getUniqueId());
        if (activeHotspot != null) return;

        // Check Database for a saved hotspot of player
        Hotspot.fromDatabase(player.getUniqueId()).thenAccept(hotspot -> {
            if (hotspot == null) {
                return; // No hotspot in database
            }

            // If expired delete from db and do nothing
            if (hotspot.hasExpired()) {
                hotspot.deleteFromDatabase();
                return;
            }

            // Load hotspot from db into queue
            HotspotQueueEvent queueEvent = new HotspotQueueEvent(hotspot, false);
            plugin.getServer().getPluginManager().callEvent(queueEvent);
            if (!queueEvent.isCancelled()) {
                queued_hotspots.add(queueEvent.getHotspot());
                Hotspots.logger().info("Found existing hotspot for player '{}'. Adding it to queue.", player.getName());
            }
        });
    }

    private void stopTimeoutIfHasHotspot(@NotNull Player player) {
        @Nullable ScheduledTask timeoutTask = timeout_tasks.getIfPresent(player.getUniqueId());
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeout_tasks.invalidate(player.getUniqueId());
        }
    }

    private void startTimeoutIfHasHotspot(@NotNull Player player) {
        // If player has no hotspot ticking or queued, don't do anything
        @Nullable Hotspot hotspot = getActiveHotspotByUUID(player.getUniqueId());
        if (hotspot == null) return;

        // Cancel previous timeout task if present (technically not needed but no harm in making sure)
        stopTimeoutIfHasHotspot(player);

        // Start timeout task and store in map
        timeout_tasks.put(player.getUniqueId(), plugin.getServer().getAsyncScheduler().runDelayed(plugin, timeout -> {
            HotspotStopTickEvent stopEvent = new HotspotStopTickEvent(
                    hotspot, HotspotStopTickEvent.Reason.PLAYER_LEAVE_TIMEOUT, player.isOnline());
            plugin.getServer().getPluginManager().callEvent(stopEvent);

            if (stopEvent.isCancelled()) {
                return;
            }

            // End hotspot, hiding the BossBar
            stopEvent.getHotspot().end();

            // Save to database if not expired
            if (!stopEvent.getHotspot().hasExpired()) {
                stopEvent.getHotspot().saveToDatabase()
                        .thenRun(() -> Hotspots.logger().info("Saved hotspot of timed out player '{}'.", player.getName()));
            }
        }, Hotspots.config().disconnect_timeout_seconds, TimeUnit.SECONDS));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerQuit(PlayerQuitEvent event) {
        startTimeoutIfHasHotspot(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerKick(PlayerKickEvent event) {
        startTimeoutIfHasHotspot(event.getPlayer());
    }

    /*
    *   Basic precondition checks
    */
    private void checkPreconditions(PlayerHotspotCreateEvent event) {
        // Check for name duplicates
        if (getActiveHotspotByName(event.getHotspot().getName()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_name_duplicate);
        }

        // Check if max active hotspot amount is reached
        else if (ticking_hotspots.size() >= Hotspots.config().max_active_hotspots) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_hotspot_limit
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%max%")
                            .replacement(Integer.toString(Hotspots.config().max_active_hotspots))
                            .build()));
        }

        // Check if player has teleported away from location
        else if (!event.getHotspot().getCenterLocation().getWorld().getUID().equals(event.getPlayer().getWorld().getUID())
                || MathHelper.square(
                event.getHotspot().getCenterLocation().getX() - event.getPlayer().getX(),
                event.getHotspot().getCenterLocation().getY() - event.getPlayer().getY(),
                event.getHotspot().getCenterLocation().getZ() - event.getPlayer().getZ()) > Hotspots.config().teleport_radius_sqared
        ) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Hotspots.translation(event.getPlayer()).create_failed_out_of_reach
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("%blocks%")
                            .replacement(Integer.toString(Hotspots.config().teleport_radius))
                            .build()));
        }

        else if (LocationUtil.isOutsideWorldBorder(event.getHotspot().getCenterLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onHotspotCreate(PlayerHotspotCreateEvent event) {
        checkPreconditions(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onHotspotConfirm(PlayerHotspotConfirmEvent event) {
        checkPreconditions(event);
    }
}
