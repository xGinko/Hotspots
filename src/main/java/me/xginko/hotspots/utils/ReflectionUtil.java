package me.xginko.hotspots.utils;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.ForName;

public final class ReflectionUtil {

    /**
     * Gets a class by its name.
     *
     * @param className a class name
     * @return a class or {@code null} if not found
     */
    @ForName
    public static @Nullable Class<?> findClass(final @NonNull String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets whether a class is loaded.
     *
     * @param className a class name
     * @return if the class is loaded
     */
    public static boolean hasClass(final @NonNull String className) {
        return findClass(className) != null;
    }
}
