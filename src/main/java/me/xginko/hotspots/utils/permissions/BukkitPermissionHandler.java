package me.xginko.hotspots.utils.permissions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitPermissionHandler implements PermissionHandler, Listener {

    private final Map<Permissible, Cache<String, TriState>> permissionCacheMap;
    private static final Duration permissionCacheDuration = Duration.ofSeconds(5);

    BukkitPermissionHandler(JavaPlugin plugin) {
        permissionCacheMap = new ConcurrentHashMap<>(Math.min(8, plugin.getServer().getOnlinePlayers().size()));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<Permissible, Cache<String, TriState>> entry : permissionCacheMap.entrySet()) {
            flushCache(entry.getKey());
        }
    }

    @Override
    public TriState permissionValue(Permissible permissible, String permission) {
        Cache<String, TriState> permCache = permissionCacheMap.computeIfAbsent(permissible, sender ->
                Caffeine.newBuilder().expireAfterWrite(permissionCacheDuration).build());
        TriState value = permCache.getIfPresent(permission);
        if (value == null) {
            value = permissible.isPermissionSet(permission) ? TriState.of(permissible.hasPermission(permission)) : TriState.UNDEFINED;
            permCache.put(permission, value);
        }
        return value;
    }

    @Override
    public void setPermission(Permissible permissible, String permission, TriState state) {
        for (PermissionAttachmentInfo attachmentInfo : permissible.getEffectivePermissions()) {
            if (attachmentInfo.getAttachment() == null) {
                continue;
            }

            if (attachmentInfo.getPermission().equals(permission)) {
                if (state == TriState.UNDEFINED) {
                    permissible.removeAttachment(attachmentInfo.getAttachment());
                } else {
                    permissible.addAttachment(attachmentInfo.getAttachment().getPlugin(), permission, state.toBoolean());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerQuit(PlayerQuitEvent event) {
        flushCache(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerKick(PlayerKickEvent event) {
        flushCache(event.getPlayer());
    }

    private void flushCache(Permissible permissible) {
        if (permissionCacheMap.containsKey(permissible)) {
            permissionCacheMap.get(permissible).invalidateAll();
            permissionCacheMap.get(permissible).cleanUp();
            permissionCacheMap.remove(permissible);
        }
    }
}
