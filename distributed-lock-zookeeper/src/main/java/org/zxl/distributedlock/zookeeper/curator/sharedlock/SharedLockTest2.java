package org.zxl.distributedlock.zookeeper.curator.sharedlock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

public class SharedLockTest2 {
    public static final String connAddr = "localhost:2181,localhost:2182,localhost:2183";
    //        public static final String connAddr = "182.168.1.109:2181,182.168.1.109:2182,182.168.1.109:2183";
    public static final int timeout = 6000;
    public static final String LOCKER_ROOT = "/locker";
    public static void main(String[] args) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connAddr)
                .sessionTimeoutMs(timeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        try {
            client.start();


            InterProcessLock interProcessLock = new InterProcessSemaphoreMutex(client, LOCKER_ROOT);
            interProcessLock.acquire(1, TimeUnit.MINUTES);
            System.out.println("do something !!!!!");
            System.out.println("输入任意键 继续");
            System.in.read();
            interProcessLock.release();

            System.out.println("输入任意键 退出");
            System.in.read();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
