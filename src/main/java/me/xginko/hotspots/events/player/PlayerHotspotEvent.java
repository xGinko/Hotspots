package me.xginko.hotspots.events.player;

import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.events.HotspotEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerHotspotEvent extends HotspotEvent {

    private final Player player;

    public PlayerHotspotEvent(@NotNull Hotspot hotspot, @NotNull Player player) {
        super(hotspot);
        this.player = player;
    }

    public PlayerHotspotEvent(boolean isAsync, @NotNull Hotspot hotspot, @NotNull Player player) {
        super(isAsync, hotspot);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
