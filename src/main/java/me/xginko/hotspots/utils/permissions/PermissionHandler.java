package me.xginko.hotspots.utils.permissions;

import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.ReflectionUtil;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

public interface PermissionHandler extends Disableable {

    static PermissionHandler create(JavaPlugin plugin) {
        if (ReflectionUtil.hasClass("net.luckperms.api.model.user.User")
                && ReflectionUtil.hasClass("net.luckperms.api.node.Node")
                && ReflectionUtil.hasClass("net.luckperms.api.util.Tristate")
                && ReflectionUtil.hasClass("net.luckperms.api.LuckPerms")) {
            return new LuckPermsPermissionHandler(plugin);
        }

        return new BukkitPermissionHandler(plugin);
    }

    void disable();
    TriState permissionValue(Permissible permissible, String permission);
    void setPermission(Permissible permissible, String permission, TriState state);

}
