package net.conczin.immersive_furniture.utils;

import java.util.concurrent.locks.ReentrantLock;

public class ScopedLock extends ReentrantLock {
    public Guard acquire() {
        lock();
        return this::unlock;
    }

    @FunctionalInterface
    public interface Guard extends AutoCloseable {
        @Override
        void close();
    }
}
