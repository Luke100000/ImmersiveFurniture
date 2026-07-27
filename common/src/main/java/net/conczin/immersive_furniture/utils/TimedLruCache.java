package net.conczin.immersive_furniture.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class TimedLruCache<K, V> {
    private final int capacity;
    private final long maxIdleNanos;
    private final LinkedHashMap<K, Entry<V>> entries;
    private final Object lock = new Object();

    public TimedLruCache(int capacity, long maxIdleNanos) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (maxIdleNanos <= 0) {
            throw new IllegalArgumentException("maxIdleNanos must be positive");
        }

        this.capacity = capacity;
        this.maxIdleNanos = maxIdleNanos;
        this.entries = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    public V computeIfAbsent(K key, Supplier<? extends V> factory) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);

        synchronized (lock) {
            long now = System.nanoTime();
            removeExpired(now);

            Entry<V> entry = entries.get(key);
            if (entry != null) {
                entry.lastAccessNanos = now;
                return entry.value;
            }
        }

        V value = Objects.requireNonNull(factory.get());

        synchronized (lock) {
            long now = System.nanoTime();
            removeExpired(now);

            Entry<V> existing = entries.get(key);
            if (existing != null) {
                existing.lastAccessNanos = now;
                return existing.value;
            }

            if (entries.size() < capacity) {
                entries.put(key, new Entry<>(value, now));
            }

            return value;
        }
    }

    private void removeExpired(long now) {
        Iterator<Map.Entry<K, Entry<V>>> iterator =
                entries.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<V> entry = iterator.next().getValue();

            if (now - entry.lastAccessNanos < maxIdleNanos) {
                break;
            }

            iterator.remove();
        }
    }

    private static final class Entry<V> {
        private final V value;
        private long lastAccessNanos;

        private Entry(V value, long lastAccessNanos) {
            this.value = value;
            this.lastAccessNanos = lastAccessNanos;
        }
    }
}