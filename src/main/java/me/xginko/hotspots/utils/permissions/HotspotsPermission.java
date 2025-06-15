package me.xginko.hotspots.utils.permissions;

import me.xginko.hotspots.Hotspots;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public enum HotspotsPermission {

    BYPASS_CREATE_COOLDOWN(new Permission("hotspots.bypass.create.cooldown", PermissionDefault.OP)),
    BYPASS_JOIN_COOLDOWN(new Permission("hotspots.bypass.join.cooldown", PermissionDefault.OP)),
    BYPASS_END_COOLDOWN(new Permission("hotspots.bypass.end.cooldown", PermissionDefault.OP)),
    BYPASS_NOTIFS_COOLDOWN(new Permission("hotspots.bypass.notifs.cooldown", PermissionDefault.OP)),
    BYPASS_CONFIRM_COOLDOWN(new Permission("hotspots.bypass.confirm.cooldown", PermissionDefault.OP)),
    BYPASS_HOTSPOT_BOUNDS(new Permission("hotspots.bypass.world-bounds", PermissionDefault.OP)),
    BYPASS_PLAYTIME_REQUIREMENT(new Permission("hotspots.bypass.playtime", PermissionDefault.OP)),

    RELOAD_CMD(new Permission("hotspots.cmd.reload", PermissionDefault.OP)),
    VERSION_CMD(new Permission("hotspots.cmd.version", PermissionDefault.OP)),
    END_CMD_OTHER(new Permission("hotspots.cmd.end.other", PermissionDefault.OP)),

    CREATE_CMD(new Permission("hotspots.cmd.create", PermissionDefault.OP)),
    END_CMD(new Permission("hotspots.cmd.end", PermissionDefault.TRUE)),
    JOIN_CMD(new Permission("hotspots.cmd.join", PermissionDefault.TRUE)),
    NOTIFS_CMD(new Permission("hotspots.cmd.notifs", PermissionDefault.TRUE));

    private final Permission permission;

    HotspotsPermission(Permission permission) {
        this.permission = permission;
    }

    public Permission get() {
        return permission;
    }

    public TriState test(Permissible permissible) {
        return Hotspots.permissions().permissionValue(permissible, permission.getName());
    }

    public static boolean registerAll() {
        boolean error = false;
        for (HotspotsPermission hotspotsPermission : HotspotsPermission.values()) {
            try {
                Hotspots.getInstance().getServer().getPluginManager().addPermission(hotspotsPermission.get());
            } catch (IllegalArgumentException e) {
                error = true;
            }
        }
        return !error;
    }

    public static void unregisterAll() {
        for (HotspotsPermission hotspotsPermission : HotspotsPermission.values()) {
            Hotspots.getInstance().getServer().getPluginManager().removePermission(hotspotsPermission.get());
        }
    }
}
