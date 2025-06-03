package me.xginko.hotspots.modules;

import com.google.common.collect.ImmutableSet;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.config.HotspotsConfig;
import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.Enableable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Module implements Enableable, Disableable {

    protected static final Set<Class<? extends Module>> AVAILABLE_MODULES;
    protected static final Map<Class<? extends Module>, Module> ENABLED_MODULES;

    static {
        AVAILABLE_MODULES = new Reflections(Module.class.getPackage().getName())
                .get(Scanners.SubTypes.of(Module.class).asClass())
                .stream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> (Class<? extends Module>) clazz)
                .sorted(Comparator.comparing(Class::getSimpleName))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableSet::copyOf));
        ENABLED_MODULES = new HashMap<>(AVAILABLE_MODULES.size());
    }

    public static <T> T get(Class<T> clazz) {
        return (T) ENABLED_MODULES.getOrDefault(clazz, null);
    }

    protected final Hotspots plugin = Hotspots.getInstance();
    protected final HotspotsConfig config = Hotspots.config();
    protected final String configPath, logFormat;
    protected final boolean enabled_in_config;

    public Module(String configPath, boolean defEnabled) {
        this(configPath, defEnabled, null);
    }

    public Module(String configPath, boolean defEnabled, String comment) {
        this.configPath = configPath;
        String[] paths = configPath.split("\\.");
        this.logFormat = "<" + (paths.length < 3 ? configPath : paths[paths.length - 2] + "." + paths[paths.length - 1]) + "> {}";
        if (comment == null || comment.isBlank()) {
            this.enabled_in_config = config.getBoolean(configPath + ".enable", defEnabled);
        } else {
            this.enabled_in_config = config.getBoolean(configPath + ".enable", defEnabled, comment);
        }
    }

    public boolean shouldEnable() {
        return enabled_in_config;
    }

    public static void disableAll() {
        for (Map.Entry<Class<? extends Module>, Module> entry : ENABLED_MODULES.entrySet()) {
            try {
                entry.getValue().disable();
            } catch (Throwable t) {
                Hotspots.logger().error("Error disabling module '{}'", entry.getKey().getSimpleName(), t);
            }
        }
        ENABLED_MODULES.clear();
    }

    public static boolean reloadModules() {
        boolean success = true;

        disableAll();

        for (Class<? extends Module> moduleClass : AVAILABLE_MODULES) {
            try {
                Module module = moduleClass.getDeclaredConstructor().newInstance();
                if (module.shouldEnable()) {
                    module.enable();
                    ENABLED_MODULES.put(moduleClass, module);
                }
            } catch (Throwable t) {
                if (t.getCause() instanceof NoClassDefFoundError) {
                    Hotspots.logger().info("Dependencies for module class {} missing, not enabling.", moduleClass.getSimpleName());
                } else {
                    success = false;
                    Hotspots.logger().warn("Failed initialising module class '{}'.", moduleClass.getSimpleName(), t);
                }
            }
        }

        return success;
    }

    protected void error(String message, Throwable throwable) {
        Hotspots.logger().error(logFormat, message, throwable);
    }

    protected void error(String message) {
        Hotspots.logger().error(logFormat, message);
    }

    protected void warn(String message) {
        Hotspots.logger().warn(logFormat, message);
    }

    protected void info(String message) {
        Hotspots.logger().info(logFormat, message);
    }

    protected void notRecognized(Class<?> clazz, String unrecognized) {
        warn("Unable to parse " + clazz.getSimpleName() + " at '" + unrecognized + "'. Please check your configuration.");
    }
}
