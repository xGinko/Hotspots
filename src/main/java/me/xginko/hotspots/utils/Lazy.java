package me.xginko.hotspots.utils;

import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<E> implements Supplier<E> {

    private final Supplier<E> supplier;
    private E value;
    private boolean initialized;

    private Lazy(Supplier<E> supplier) {
        this.supplier = supplier;
    }

    public static <E> Lazy<E> of(Supplier<E> supplier) {
        Objects.requireNonNull(supplier, "Can't create lazy if supplier is null!");
        return new Lazy<>(supplier);
    }

    public boolean isInitialized() {
        return initialized || !this.isEmpty();
    }

    public boolean isEmpty() {
        return this.value == null;
    }

    public void clear() {
        this.value = null;
    }

    @Override
    public E get() {
        if (this.isInitialized()) {
            return this.value;
        }

        try {
            this.value = this.supplier.get();
        } catch (Throwable ignored) {
        }

        this.initialized = true;
        return this.value;
    }
}
