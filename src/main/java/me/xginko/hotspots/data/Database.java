package me.xginko.hotspots.data;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.utils.Disableable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Database implements Disableable {

    protected final Hotspots plugin = Hotspots.getInstance();

    public abstract boolean createTables();
    @NotNull
    public abstract CompletableFuture<@NotNull Boolean> getNotificationsEnabled(@NotNull UUID uuid);
    @NotNull
    public abstract CompletableFuture<@NotNull Boolean> setNotificationsEnabled(@NotNull UUID player, boolean enable);
    @NotNull
    public abstract CompletableFuture<@NotNull Set<Hotspot>> getAllHotspots();
    @NotNull
    public abstract CompletableFuture<@NotNull Boolean> saveHotspot(@NotNull Hotspot hotspot);
    @NotNull
    public abstract CompletableFuture<@Nullable Hotspot> getHotspot(@NotNull UUID uuid);
    @NotNull
    public abstract CompletableFuture<@NotNull Boolean> deleteHotspot(@NotNull UUID uuid);

}
