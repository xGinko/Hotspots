package me.xginko.hotspots.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.utils.BossBarUtil;
import me.xginko.hotspots.utils.LocationUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SQLiteDatabase extends Database {

    private final @NotNull DataSource dataSource;

    public SQLiteDatabase() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().toPath() + "/data.db");
        hikariConfig.setMaximumPoolSize(Hotspots.config().database_max_pool_size);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    public void disable() {
        try {
            dataSource.getConnection().close();
        } catch (SQLException e) {
            Hotspots.logger().error("Error closing database connection.", e);
            plugin.onException();
        }
    }

    @Override
    public boolean createTables() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `hotspots` (" +
                    "`player_uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "`location` VARCHAR(256), " +
                    "`creation_time_millis` LONG, " +
                    "`life_time_millis` LONG, " +
                    "`bossbar` VARCHAR(512));");
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `notifications` (" +
                    "`player_uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "`show` boolean DEFAULT TRUE);");
            return true;
        } catch (SQLException e) {
            Hotspots.logger().error("Error creating tables in database!", e);
            plugin.onException();
            return false;
        }
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> getNotificationsEnabled(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM `notifications` WHERE player_uuid = ?;"
            )) {
                statement.setString(1, uuid.toString());
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    return result.getBoolean("show");
                }
            } catch (Exception e) {
                Hotspots.logger().error("Error getting notification setting from database! (uuid={})", uuid, e);
                plugin.onException();
            }
            return true;
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> setNotificationsEnabled(@NotNull UUID uuid, boolean enable) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `notifications` (player_uuid, show) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET show = ?;"
            )) {
                statement.setString(1, uuid.toString());
                statement.setBoolean(2, enable);
                statement.setBoolean(3, enable);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Hotspots.logger().error("Error saving notification setting to database! (uuid={})", uuid, e);
                plugin.onException();
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Set<Hotspot>> getAllHotspots() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM `hotspots`;"
            )) {
                Set<Hotspot> hotspots = new HashSet<>();
                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(result.getString("player_uuid"));
                    } catch (IllegalArgumentException e) {
                        Hotspots.logger().warn("Couldn't parse UUID from database. This should never happen.");
                        plugin.onException();
                        continue;
                    }

                    long creationTime = result.getLong("creation_time_millis");
                    if (System.currentTimeMillis() - creationTime >= Hotspots.config().hotspot_max_store_time) {
                        deleteHotspot(uuid);
                        continue;
                    }

                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null) { // Player not online
                        continue;
                    }

                    Location location;
                    try {
                        location = LocationUtil.fromJSONString(result.getString("location"));
                    } catch (Exception e) {
                        Hotspots.logger().error("Failed parsing Location from JSON string for player {}", player.getName(), e);
                        plugin.onException();
                        continue;
                    }

                    BossBar bossBar;
                    try {
                        bossBar = BossBarUtil.fromJSONString(result.getString("bossbar"));
                    } catch (Exception e) {
                        Hotspots.logger().error("Failed parsing BossBar from JSON string for player {}", player.getName(), e);
                        plugin.onException();
                        continue;
                    }

                    hotspots.add(new Hotspot(player, location, result.getLong("life_time_millis"), creationTime, bossBar));
                }
                return hotspots;
            } catch (Exception e) {
                Hotspots.logger().error("Error while getting all possible hotspots from database!", e);
                plugin.onException();
                return Collections.emptySet();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> saveHotspot(@NotNull Hotspot hotspot) {
        return CompletableFuture.supplyAsync(() -> {
            final String location = LocationUtil.toJSONString(hotspot.getCenterLocation());
            final String bossBar = BossBarUtil.toJSONString(hotspot.getBossBar());
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `hotspots` (player_uuid, location, creation_time_millis, life_time_millis, bossbar) VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT(player_uuid) DO UPDATE SET location = ?, creation_time_millis = ?, life_time_millis = ?, bossbar = ?;"
            )) {
                statement.setString(1, hotspot.getOwnerUUID().toString());
                statement.setString(2, location);
                statement.setLong(3, hotspot.getCreationTime());
                statement.setLong(4, hotspot.getAliveTimeMillis());
                statement.setString(5, bossBar);
                statement.setString(6, location);
                statement.setLong(7, hotspot.getCreationTime());
                statement.setLong(8, hotspot.getAliveTimeMillis());
                statement.setString(9, bossBar);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Hotspots.logger().error("Error saving hotspot '{}' by player {}!", hotspot.getName(), hotspot.getOwnerName(), e);
                plugin.onException();
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Hotspot> getHotspot(@NotNull UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            Hotspots.logger().warn("Tried to get hotspot from database but player wasn't online (uuid={})", uuid);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM `hotspots` WHERE player_uuid = ?;"
            )) {
                statement.setString(1, uuid.toString());
                ResultSet result = statement.executeQuery();

                if (!result.next()) {
                    return null;
                }

                long creationTime = result.getLong("creation_time_millis");
                if (System.currentTimeMillis() - creationTime >= Hotspots.config().hotspot_max_store_time) {
                    deleteHotspot(uuid);
                    return null;
                }

                Location location;
                try {
                    location = LocationUtil.fromJSONString(result.getString("location"));
                } catch (Exception e) {
                    Hotspots.logger().error("Failed parsing Location from JSON string for player {}", player.getName(), e);
                    plugin.onException();
                    return null;
                }

                BossBar bossBar;
                try {
                    bossBar = BossBarUtil.fromJSONString(result.getString("bossbar"));
                } catch (Exception e) {
                    Hotspots.logger().error("Failed parsing BossBar from JSON string for player {}", player.getName(), e);
                    plugin.onException();
                    return null;
                }

                return new Hotspot(player, location, result.getLong("life_time_millis"), creationTime, bossBar);
            } catch (Exception e) {
                Hotspots.logger().error("Error getting hotspot from database for uuid={}!", uuid, e);
                plugin.onException();
                return null;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> deleteHotspot(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `hotspots` WHERE player_uuid = ?;"
            )) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Hotspots.logger().error("Error while deleting hotspot from database! (uuid={})", uuid, e);
                plugin.onException();
                return false;
            }
        });
    }
}
