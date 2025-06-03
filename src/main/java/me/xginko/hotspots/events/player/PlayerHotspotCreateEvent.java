package me.xginko.hotspots.events.player;

import me.xginko.hotspots.Hotspot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerHotspotCreateEvent extends PlayerHotspotEvent implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();

    private final @NotNull Reason reason;
    private boolean cancelled;

    public PlayerHotspotCreateEvent(@NotNull Hotspot hotspot, @NotNull Player player, @NotNull Reason reason, boolean cancelled) {
        super(hotspot, player);
        this.reason = reason;
        this.cancelled = cancelled;
    }

    public PlayerHotspotCreateEvent(boolean isAsync, @NotNull Hotspot hotspot, @NotNull Player player, @NotNull Reason reason, boolean cancelled) {
        super(isAsync, hotspot, player);
        this.reason = reason;
        this.cancelled = cancelled;
    }

    public @NotNull Reason getReason() {
        return reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public enum Reason { COMMAND, OTHER }
}
