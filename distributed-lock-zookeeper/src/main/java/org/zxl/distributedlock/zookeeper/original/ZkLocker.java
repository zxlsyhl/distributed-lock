package org.zxl.distributedlock.zookeeper.original;

import org.apache.zookeeper.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public class ZkLocker implements Locker{

    public void lock(String key, Runnable runnable) {
        ZkLockerWatcher watcher = ZkLockerWatcher.conn(key);
        try {
            if(watcher.getLock()){
                runnable.run();
            }
        }finally {
            watcher.releaseLock();
        }
    }

    private static class ZkLockerWatcher implements Watcher {
        public static final String connAddr = "localhost:2181,localhost:2182,localhost:2183";
//        public static final String connAddr = "182.168.1.109:2181,182.168.1.109:2182,182.168.1.109:2183";
        public static final int timeout = 6000;
        public static final String LOCKER_ROOT = "/locker";
        private static String lock = "0";

        ZooKeeper zooKeeper;
        String parentLockPath;
        String childLockPath;
        Thread thread;
        CountDownLatch cdl = new CountDownLatch(1);

        public static ZkLockerWatcher conn(String key){
            ZkLockerWatcher watcher = new ZkLockerWatcher();

            try {
                ZooKeeper zooKeeper = watcher.zooKeeper = new ZooKeeper(connAddr,timeout,watcher);
                watcher.cdl.await();
//                System.out.println("连接建立成功****************************************************");
                watcher.thread = Thread.currentThread();
                //阻塞等待连接建立完毕
//                LockSupport.park();

                //同时只有一个线程操作
                synchronized (lock){
                    //根节点如果不存在，就创建一个（并发问题，如果两个线程同时检测不存在，两个同时去创建必须有一个会失败）
                    if(zooKeeper.exists(LOCKER_ROOT, false) == null){
                        try {
                            zooKeeper.create(LOCKER_ROOT, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        }catch (KeeperException e){
                            System.out.println("创建根节点节点失败"+LOCKER_ROOT);
                            e.printStackTrace();
                        }
                    }
                    //当前枷锁的节点是否存在
                    watcher.parentLockPath = LOCKER_ROOT + "/"+ key;
                    if(zooKeeper.exists(watcher.parentLockPath, false) == null){
                        try {
                            zooKeeper.create(watcher.parentLockPath, "".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        }catch (KeeperException e){
                            System.out.println("创建key节点失败"+watcher.parentLockPath);
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("conn to zk error"+ e);
                throw new RuntimeException("conn to zk error");
            } finally {
            }
            return watcher;
        }

        public boolean getLock(){
            try {
                //创建子节点
                System.out.println("创建临时顺序节点开始 ");
                this.childLockPath = zooKeeper.create(parentLockPath+"/","".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                System.out.println("创建临时顺序节点结束");
                //检查自己是不是最小的节点，是则获取成功，不是则监听上一个节点
                return getLockOrWatchLast();
            } catch (Exception e) {
                System.out.println("get lock error");
                e.printStackTrace();
                throw new RuntimeException("get lock error");
            } finally {
            }
        }

        public void releaseLock(){
            try {
                if(childLockPath != null){
                    //释放锁，删除节点
                    zooKeeper.delete(childLockPath, -1);
                }
                //最后一个释放的删除锁节点
                List<String> children = zooKeeper.getChildren(parentLockPath,false);

                if(children.isEmpty()){
                    try {
                        zooKeeper.delete(parentLockPath, -1);
                    } catch (KeeperException e) {
                        System.out.println();
                        e.printStackTrace();
                    }
                }
                //关闭zk连接
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            }catch (Exception e){
                System.out.println("release lock error");
                throw new RuntimeException("release lock error");

            }
            finally {

            }
        }

        public boolean getLockOrWatchLast() throws KeeperException, InterruptedException {
            List<String> children = zooKeeper.getChildren(parentLockPath,false);
            //必须要排序一下，这里取出的顺序可能是乱的
            Collections.sort(children);
            if(children.size() > 0){
                //如果当前节点是第一个子节点，则获取成功
                if ((parentLockPath + "/" + children.get(0)).equals(childLockPath)){
                    return true;
                }
            }

            //如果不是第一个子节点，则就监听前一个节点
            String last = "";
            for(String child : children){
                if((parentLockPath + "/" + child).equals(childLockPath)){
                    break;
                }
                last = child;
            }

            if(zooKeeper.exists(parentLockPath + "/"+ last, true) != null){
                this.thread = Thread.currentThread();
                //阻塞当前线程
                LockSupport.park(this.thread);
                //唤醒之后重新检测自己是不是最小的节点，因为有可能上一个节点断线了
                return getLockOrWatchLast();
            }
            else{
                //如果上一个节点不存在，说明还没来得及监听就释放了，重新检查一次。
                return getLockOrWatchLast();
            }
        }

        @Override
        public  void process(WatchedEvent event) {
            //连接请求监听
            if(event.getState() == Event.KeeperState.SyncConnected){
                System.out.println("连接成功***********************************************");
                cdl.countDown();
            }

            if(this.thread != null && event.getType() == Event.EventType.NodeDeleted){
                System.out.println("删除节点："+event.getPath());
                System.out.println("唤醒进程******************************************");
                //唤醒阻塞的线程（这是在监听线程，跟获取锁的线程不是同一个线程）
                LockSupport.unpark(this.thread);
                this.thread = null;
            }


        }
    }
}
