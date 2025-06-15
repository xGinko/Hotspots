package me.xginko.hotspots.utils.permissions;

import org.checkerframework.checker.nullness.qual.NonNull;

public enum TriState {

    UNDEFINED(false),
    FALSE(false),
    TRUE(true);

    private final boolean booleanValue;

    TriState(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean toBoolean() {
        return this.booleanValue;
    }

    public static @NonNull TriState of(boolean val) {
        return val ? TRUE : FALSE;
    }

    public static @NonNull TriState of(Boolean val) {
        return val == null ? UNDEFINED : (val ? TRUE : FALSE);
    }
}
