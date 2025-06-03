package me.xginko.hotspots;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.managers.NotificationManager;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.modules.requirements.WorldBounds;
import me.xginko.hotspots.utils.BossBarUtil;
import me.xginko.hotspots.utils.LocationUtil;
import me.xginko.hotspots.utils.MathHelper;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class Hotspot {

    private static final @NotNull Cache<Hotspot, List<Location>> TELEPORT_LOCATION_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(8))
            .<Hotspot, List<Location>>evictionListener((hotspot, locations, cause) -> {
                if (locations != null) {
                    locations.clear();
                }
            })
            .build();

    private final @NotNull UUID creator;
    private final @NotNull String hotspotName, creatorName;
    private @NotNull Location centerLocation;
    private @NotNull BossBar bossBar;
    private long creationTime, lastTickTime, aliveTimeMillis;
    private boolean dead;

    public Hotspot(
            @NotNull UUID creator, @NotNull Location centerLocation, @NotNull String creatorName,
            @NotNull String hotspotName, @NotNull BossBar.Color color, long creationTime, long aliveTimeMillis
    ) {
        this(creator, hotspotName, creatorName, centerLocation, BossBarUtil.getBossBar(hotspotName, color), creationTime , aliveTimeMillis);
    }

    public Hotspot(
            @NotNull UUID creator, @NotNull String hotspotName, @NotNull String creatorName, @NotNull Location centerLocation,
            @NotNull BossBar bossBar, long creationTime, long aliveTimeMillis
    ) {
        this.creator = creator;
        this.hotspotName = hotspotName;
        this.creatorName = creatorName;
        this.centerLocation = centerLocation;
        this.bossBar = bossBar;
        this.creationTime = creationTime;
        this.lastTickTime = 0L;
        this.aliveTimeMillis = aliveTimeMillis;
        this.dead = false;
    }

    public Hotspot(@NotNull Player creator, @NotNull Location centerLocation, long aliveTimeMillis, long creationTime, BossBar bossBar) {
        this(
                creator.getUniqueId(),
                PlainTextComponentSerializer.plainText().serialize(bossBar.name()),
                PlainTextComponentSerializer.plainText().serialize(creator.displayName()),
                centerLocation,
                bossBar,
                creationTime,
                aliveTimeMillis
        );
    }

    public Hotspot(@NotNull Player creator, BossBar bossBar) {
        this(
                creator.getUniqueId(),
                PlainTextComponentSerializer.plainText().serialize(bossBar.name()),
                PlainTextComponentSerializer.plainText().serialize(creator.displayName()),
                creator.getLocation().toCenterLocation(),
                bossBar,
                System.currentTimeMillis(),
                0L
        );
    }

    public static @NotNull CompletableFuture<@Nullable Hotspot> fromDatabase(@NotNull UUID playerUniqueId) {
        return Hotspots.database().getHotspot(playerUniqueId);
    }

    public void tick() {
        if (dead) return;

        // Handle BossBar visibility each tick. This works most reliably
        if (Manager.get(NotificationManager.class) != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Manager.get(NotificationManager.class).getNotificationsEnabled(player.getUniqueId()).thenAccept(enabled -> {
                    if (enabled) player.showBossBar(bossBar);
                    else player.hideBossBar(bossBar);
                });
            }
        }

        // Don't count alive time while the last time it was ticked hasn't been recorded yet
        if (lastTickTime != 0L) aliveTimeMillis += Math.max(0L, System.currentTimeMillis() - lastTickTime);
        lastTickTime = System.currentTimeMillis();

        // Calculate progress
        float progress = MathHelper.clamp((float) aliveTimeMillis / (float) Hotspots.config().hotspot_life_time, BossBar.MIN_PROGRESS, BossBar.MAX_PROGRESS);
        bossBar.progress(Hotspots.config().bossbar_reverse_progress ? BossBar.MAX_PROGRESS - progress : progress);
    }

    public void end() {
        if (dead) return;

        // Cleanup BossBar
        for (Audience audience : Bukkit.getOnlinePlayers()) {
            audience.hideBossBar(bossBar);
        }

        // Invalidate any cached teleport locations
        TELEPORT_LOCATION_CACHE.invalidate(this);

        dead = true;
    }

    public @NotNull CompletableFuture<@NotNull Location> getTeleportLocation() {
        List<Location> locations = TELEPORT_LOCATION_CACHE.get(this, k -> Collections.synchronizedList(new ArrayList<>()));

        if (!locations.isEmpty()) {
            // If there already are generated locations available, complete future with a random one from the list
            return CompletableFuture.completedFuture(Util.getRandomElement(locations));
        }

        CompletableFuture<@NotNull Location> future = new CompletableFuture<>();

        Hotspots plugin = Hotspots.getInstance();
        plugin.getServer().getRegionScheduler().execute(plugin, centerLocation, () -> {
            final int radius = Hotspots.config().teleport_radius;

            final World world = centerLocation.getWorld();
            final int centerX = centerLocation.getBlockX();
            final int centerY = centerLocation.getBlockY();
            final int centerZ = centerLocation.getBlockZ();

            // Iterate from center location in a cuboid shape using the configured teleport radius
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    for (int y = Math.max(world.getMinHeight(), centerY - radius); y <= Math.min(world.getMaxHeight(), centerY + radius); y++) {
                        final Block block = world.getBlockAt(x, y, z);

                        if (isInvalidTeleportLocation(block.getLocation())) {
                            continue;
                        }

                        // Check if the block could be used for a safe pocket.
                        // If it's air or water, it's a good potential position for the player's legs
                        if (!block.getType().isAir() && block.getType() != Material.WATER) {
                            continue;
                        }

                        // Check the block which would be used for the player's torso and head as well
                        Block above = block.getRelative(BlockFace.UP);
                        if (!above.getType().isAir() && above.getType() != Material.WATER) {
                            continue;
                        }

                        // Finally make sure the block below the player's feet is solid
                        if (block.getRelative(BlockFace.DOWN).isSolid()) {
                            locations.add(LocationUtil.toXZCenter(block.getLocation()));
                        }
                    }
                }
            }

            if (locations.isEmpty()) {
                // If we cant find any safe locations, we just create 10 random ones, ignoring how safe/unsafe they might be
                for (int i = 0; i < 10; i++) {
                    int tries = 100;
                    Location generatedLoc;
                    do {
                        generatedLoc = LocationUtil.toXZCenter(centerLocation).add(
                                Util.RANDOM.nextInt(-radius, radius),
                                Util.RANDOM.nextInt(-radius, radius),
                                Util.RANDOM.nextInt(-radius, radius));
                        tries--;
                        if (tries < 0) {
                            Hotspots.logger().warn(
                                    "Failed to generate a valid teleport location after not finding " +
                                    "any safe locations for hotspot '{}' of player '{}'. " +
                                    "Using invalid location. This could mean players die or fail to teleport.", hotspotName, creatorName);
                            break; // Prevent loop getting stuck by any chance
                        }
                    } while (isInvalidTeleportLocation(generatedLoc));

                    locations.add(generatedLoc);
                }
            }

            // Complete future with a randomly selected location from the generated list
            future.complete(Util.getRandomElement(locations));
        });

        return future;
    }

    public boolean isInvalidTeleportLocation(Location location) {
        WorldBounds worldBounds = Module.get(WorldBounds.class);
        if (worldBounds != null && worldBounds.isOutOfBounds(location)) {
            return true;
        }
        return !contains(location) || !location.getWorld().getWorldBorder().isInside(location);
    }

    public @NotNull UUID getOwnerUUID() {
        return creator;
    }

    public @NotNull String getOwnerName() {
        return creatorName;
    }

    public @NotNull String getName() {
        return hotspotName;
    }

    public @NotNull Location getCenterLocation() {
        return centerLocation;
    }

    public @NotNull BossBar getBossBar() {
        return bossBar;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getAliveTimeMillis() {
        return aliveTimeMillis;
    }

    public boolean hasExpired() {
        return aliveTimeMillis >= Hotspots.config().hotspot_life_time;
    }

    public boolean hasEnded() {
        return dead;
    }

    public boolean contains(@NotNull Location location) {
        if (!location.getWorld().getUID().equals(centerLocation.getWorld().getUID()))
            return false;

        return MathHelper.square(
                location.getX() - centerLocation.getX(),
                location.getY() - centerLocation.getY(),
                location.getZ() - centerLocation.getZ()) <= Hotspots.config().teleport_radius_sqared;
    }

    public void setCenterLocation(@NotNull Location centerLocation) {
        this.centerLocation = centerLocation;
    }

    public void setBossBar(@NotNull BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setAliveTimeMillis(long aliveTimeMillis) {
        this.aliveTimeMillis = aliveTimeMillis;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public @NotNull CompletableFuture<Boolean> saveToDatabase() {
        return Hotspots.database().saveHotspot(this);
    }

    public @NotNull CompletableFuture<Boolean> deleteFromDatabase() {
        return Hotspots.database().deleteHotspot(creator);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Hotspot hotspot = (Hotspot) o;
        return Objects.equals(creator, hotspot.creator);
    }

    @Override
    public int hashCode() {
        return creator.hashCode();
    }
}
