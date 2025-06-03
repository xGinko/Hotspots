package me.xginko.hotspots.managers;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.config.HotspotsConfig;
import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.Enableable;

import java.util.HashMap;
import java.util.Map;

public abstract class Manager implements Enableable, Disableable {

    protected final Hotspots plugin = Hotspots.getInstance();
    protected final HotspotsConfig config = Hotspots.config();

    public static final Map<Class<? extends Manager>, Manager> MANAGERS = new HashMap<>(3);

    public static <T> T get(Class<T> clazz) {
        return (T) MANAGERS.getOrDefault(clazz, null);
    }

    public static void disableAll() {
        for (Map.Entry<Class<? extends Manager>, Manager> entry : MANAGERS.entrySet()) {
            try {
                entry.getValue().disable();
            } catch (Throwable t) {
                Hotspots.logger().error("Error disabling manager '{}'", entry.getKey().getSimpleName(), t);
            }
        }
        MANAGERS.clear();
    }

    public static boolean reloadManagers() {
        boolean noErrors = true;
        try {
            disableAll();

            MANAGERS.put(NotificationManager.class, new NotificationManager());
            MANAGERS.put(HotspotManager.class, new HotspotManager());
            MANAGERS.put(WarmupManager.class, new WarmupManager());

            for (Map.Entry<Class<? extends Manager>, Manager> manager : MANAGERS.entrySet()) {
                manager.getValue().enable();
            }
        } catch (Throwable t) {
            Hotspots.logger().error("Error reloading Managers!", t);
            noErrors = false;
        }
        return noErrors;
    }
}
