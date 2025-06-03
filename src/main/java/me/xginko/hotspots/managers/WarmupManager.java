package me.xginko.hotspots.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class WarmupManager extends Manager implements Listener {

    private final @NotNull Cache<UUID, TeleportWarmup> warmups;

    public WarmupManager() {
        this.warmups = Caffeine.newBuilder().expireAfterWrite(Hotspots.config().teleport_warmup_seconds).build();
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, TeleportWarmup> warmupEntry : warmups.asMap().entrySet()) {
            warmupEntry.getValue().cancelWithMessage();
            warmups.invalidate(warmupEntry.getKey());
        }
        warmups.cleanUp();
    }

    public void startWarmup(Player player, Hotspot hotspot) {
        warmups.put(player.getUniqueId(), new TeleportWarmup(player, hotspot));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onDamaged(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        @Nullable TeleportWarmup warmup = warmups.getIfPresent(event.getEntity().getUniqueId());
        if (warmup != null) {
            warmup.cancelWithMessage();
            warmups.invalidate(event.getEntity().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onMoved(PlayerMoveEvent event) {
        @Nullable TeleportWarmup warmup = warmups.getIfPresent(event.getPlayer().getUniqueId());
        if (warmup == null) return;

        if (
                (event.getTo().getBlockX() != event.getFrom().getBlockX())
                || (event.getTo().getBlockY() != event.getFrom().getBlockY())
                || (event.getTo().getBlockZ() != event.getFrom().getBlockZ())
        ) { // Only cancel if player has made a significant change to their position
            warmup.cancelWithMessage();
            warmups.invalidate(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onQuit(PlayerQuitEvent event) {
        @Nullable TeleportWarmup warmup = warmups.getIfPresent(event.getPlayer().getUniqueId());
        if (warmup != null) {
            warmup.cancel();
            warmups.invalidate(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onKicked(PlayerKickEvent event) {
        @Nullable TeleportWarmup warmup = warmups.getIfPresent(event.getPlayer().getUniqueId());
        if (warmup != null) {
            warmup.cancel();
            warmups.invalidate(event.getPlayer().getUniqueId());
        }
    }

    public static final class TeleportWarmup implements Consumer<ScheduledTask> {

        private final @NotNull Player player;
        private final @NotNull Hotspot hotspot;
        private final @NotNull ScheduledTask countdownTask;
        private @NotNull Duration timeLeft;

        public TeleportWarmup(@NotNull Player player, @NotNull Hotspot hotspot) {
            this.player = player;
            this.timeLeft = Hotspots.config().teleport_warmup_seconds;
            this.hotspot = hotspot;
            this.countdownTask = Hotspots.getInstance().getServer().getAsyncScheduler()
                    .runAtFixedRate(Hotspots.getInstance(), this, 1L, 1000L, TimeUnit.MILLISECONDS);
        }

        @Override
        public void accept(ScheduledTask task) {
            if (hotspot.hasEnded() || hotspot.hasExpired()) {
                cancelWithMessage();
                return;
            }

            if (!timeLeft.isZero() && !timeLeft.isNegative()) {
                final TextReplacementConfig waitTime = TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(Util.formatDuration(timeLeft))
                        .build();
                player.showTitle(Title.title(
                        Hotspots.translation(player).join_warmup_title.replaceText(waitTime),
                        Hotspots.translation(player).join_warmup_subtitle.replaceText(waitTime),
                        Hotspots.config().teleport_warmup_times));
                timeLeft = timeLeft.minusSeconds(1);
                return;
            }

            hotspot.getTeleportLocation().thenAccept(location -> player.teleportAsync(location).thenRun(() ->
                    player.sendMessage(Hotspots.translation(player).join_success
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%hotspot_name%").replacement(hotspot.getName()).build()))
            ));

            cancel();
        }

        public void cancelWithMessage() {
            player.sendMessage(Hotspots.translation(player).join_warmup_cancelled);
            cancel();
        }

        public void cancel() {
            countdownTask.cancel();
        }
    }
}
