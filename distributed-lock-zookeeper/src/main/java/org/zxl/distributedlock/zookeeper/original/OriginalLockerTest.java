package org.zxl.distributedlock.zookeeper.original;

import java.io.IOException;

public class OriginalLockerTest {
    private static Locker locker = new ZkLocker();
    public static void main(String[] args) throws IOException {
        for(int i = 0; i < 10; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    locker.lock("user_1", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println(String.format("user_1 time: %d, threadName: %s", System.currentTimeMillis(), Thread.currentThread().getName()));
                                Thread.sleep(500);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            },"Thread-"+i).start();
        }

        for(int i = 10; i < 20; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    locker.lock("user_3", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println(String.format("user_3 time: %d, threadName: %s", System.currentTimeMillis(), Thread.currentThread().getName()));
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }, "Thread-" + i).start();
        }

        System.in.read();
    }
}
