package org.zxl.distributedlock.zookeeper.original;

public interface Locker {
    void lock(String key, Runnable runnable);
}
