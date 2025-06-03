package me.xginko.hotspots.events.player;

import me.xginko.hotspots.Hotspot;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerHotspotConfirmEvent extends PlayerHotspotCreateEvent {

    private static final @NotNull HandlerList handlers = new HandlerList();

    public PlayerHotspotConfirmEvent(@NotNull Hotspot hotspot, @NotNull Player player, boolean cancelled) {
        super(hotspot, player, Reason.COMMAND, cancelled);
    }

    public PlayerHotspotConfirmEvent(boolean isAsync, @NotNull Hotspot hotspot, @NotNull Player player, boolean cancelled) {
        super(isAsync, hotspot, player, Reason.COMMAND, cancelled);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
