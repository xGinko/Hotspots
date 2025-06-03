package me.xginko.hotspots.events;

import me.xginko.hotspots.Hotspot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HotspotStartTickEvent extends HotspotEvent implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();

    private boolean cancelled;

    public HotspotStartTickEvent(@NotNull Hotspot hotspot, boolean cancelled) {
        super(hotspot);
        this.cancelled = cancelled;
    }

    public HotspotStartTickEvent(boolean isAsync, @NotNull Hotspot hotspot, boolean cancelled) {
        super(isAsync, hotspot);
        this.cancelled = cancelled;
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
