package me.xginko.hotspots.utils;

public final class MathHelper {

    public static double square(double delta) {
        return delta * delta;
    }

    public static double square(double deltaX, double deltaZ) {
        return Math.fma(deltaX, deltaX, deltaZ * deltaZ);
    }

    public static double square(double deltaX, double deltaY, double deltaZ) {
        return Math.fma(deltaX, deltaX, Math.fma(deltaY, deltaY, deltaZ * deltaZ));
    }

    public static double fma(double a, double b, double c) {
        return Math.fma(a, b, c);
    }

    public static double sin(double rad) {
        return Math.sin(rad);
    }

    public static double cos(double rad) {
        return Math.cos(rad);
    }

    public static double acos(double rad) {
        return Math.acos(rad);
    }

    public static double clamp(double val, double min, double max) {
        return Math.clamp(val, min, max); // This clamp method is different from java lang Math :I
    }

    public static float clamp(float val, float min, float max) {
        return Math.clamp(val, min, max); // This clamp method is different from java lang Math :I
    }
}
