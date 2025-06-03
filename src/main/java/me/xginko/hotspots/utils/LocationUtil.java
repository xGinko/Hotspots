package me.xginko.hotspots.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.UUID;

public final class LocationUtil {

    public static @NotNull String toHumanString(@NotNull Location location) {
        return "[" + location.getWorld().getName() + "] x=" + location.getBlockX() + ", y=" + location.getBlockY() + ", z=" + location.getBlockZ();
    }

    public static @NotNull Location toXZCenter(@NotNull Location location) {
        Location centered = location.clone(); // Clone so we don't alter original location
        centered.setX(centered.getBlockX() + 0.5D);
        centered.setY(centered.getBlockY());
        centered.setZ(centered.getBlockZ() + 0.5D);
        return centered;
    }

    public static @NotNull Location clampToBounds(@NotNull Location location) { // Recreated from minecraft internals
        Location clamped = location.clone(); // Clone so we don't alter original location
        Location borderCenter = clamped.getWorld().getWorldBorder().getCenter();
        double borderHalfSize = clamped.getWorld().getWorldBorder().getSize() / 2.0D;
        clamped.setX(MathHelper.clamp(clamped.getX(), borderCenter.getX() - borderHalfSize, (borderCenter.getX() + borderHalfSize) - 1.0D));
        clamped.setY(MathHelper.clamp(clamped.getY(), clamped.getWorld().getMinHeight(), clamped.getWorld().getMaxHeight()));
        clamped.setZ(MathHelper.clamp(clamped.getZ(), borderCenter.getZ() - borderHalfSize, (borderCenter.getZ() + borderHalfSize) - 1.0D));
        return clamped;
    }

    public static boolean isOutsideWorldBorder(@NotNull Location location) {
        return !location.getWorld().getWorldBorder().isInside(location);
    }

    public static @NotNull String toJSONString(@NotNull Location location) {
        JSONObject json = new JSONObject();
        json.put("world", location.getWorld().getUID().toString());
        json.put("x", location.getX());
        json.put("y", location.getY());
        json.put("z", location.getZ());
        return json.toJSONString();
    }

    public static @NotNull Location fromJSONString(@NotNull String json_string) throws NullPointerException, ParseException {
        JSONObject json = (JSONObject) new JSONParser().parse(json_string);
        return new Location(
                Bukkit.getWorld(UUID.fromString((String) json.get("world"))),
                (Double) json.get("x"),
                (Double) json.get("y"),
                (Double) json.get("z"));
    }

    public static double getBlockDistanceTo00Squared(Location location) {
        return MathHelper.square(location.getX(), location.getZ());
    }
}
