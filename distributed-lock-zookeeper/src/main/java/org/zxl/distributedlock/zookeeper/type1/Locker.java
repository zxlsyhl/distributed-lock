package org.zxl.distributedlock.zookeeper.type1;

public interface Locker {
    void lock(String key, Runnable runnable);
}
