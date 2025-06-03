package me.xginko.hotspots.events;

import me.xginko.hotspots.Hotspot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HotspotStopTickEvent extends HotspotEvent implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();

    private final @NotNull Reason reason;
    private boolean cancelled;

    public HotspotStopTickEvent(@NotNull Hotspot hotspot, @NotNull Reason reason, boolean cancelled) {
        super(hotspot);
        this.reason = reason;
        this.cancelled = cancelled;
    }

    public HotspotStopTickEvent(boolean isAsync, @NotNull Hotspot hotspot, @NotNull Reason reason, boolean cancelled) {
        super(isAsync, hotspot);
        this.reason = reason;
        this.cancelled = cancelled;
    }

    public enum Reason { EXPIRED, CANCELLED, PLAYER_LEAVE_TIMEOUT, PLAYER_DEATH, OTHER }

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
}
