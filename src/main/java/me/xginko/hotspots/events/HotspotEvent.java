package me.xginko.hotspots.events;

import me.xginko.hotspots.Hotspot;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class HotspotEvent extends Event {

    private final @NotNull Hotspot hotspot;

    public HotspotEvent(@NotNull Hotspot hotspot) {
        super(true); // We expect hotspots to always tick async
        this.hotspot = hotspot;
    }

    public HotspotEvent(boolean isAsync, @NotNull Hotspot hotspot) {
        super(isAsync);
        this.hotspot = hotspot;
    }

    public @NotNull Hotspot getHotspot() {
        return hotspot;
    }
}
