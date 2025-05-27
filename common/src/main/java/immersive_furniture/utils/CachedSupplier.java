package immersive_furniture.utils;

import java.util.function.Supplier;

public class CachedSupplier<T> implements Supplier<T> {
    private final Supplier<T> delegate;
    private T value;
    private boolean initialized = false;

    public CachedSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    public synchronized T get() {
        if (!initialized) {
            value = delegate.get();
            initialized = true;
        }
        return value;
    }
}
