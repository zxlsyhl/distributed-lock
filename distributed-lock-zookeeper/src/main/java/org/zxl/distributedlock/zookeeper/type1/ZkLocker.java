package org.zxl.distributedlock.zookeeper.type1;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class ZkLocker implements Locker{

    public void lock(String key, Runnable runnable) {

    }

    private static class ZkLockerWatcher implements Watcher {

        public void process(WatchedEvent watchedEvent) {


        }
    }
}
