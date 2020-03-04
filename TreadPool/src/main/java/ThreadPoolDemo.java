import java.util.concurrent.*;

public class ThreadPoolDemo implements Runnable {

    /**
     * java中默认提供了几种线程池，及其作用
     * 线程池需要哪些参数，这些参数的含义
     *
     * @param args
     */
    public static void main(String args[]) {


//      Executors.newScheduledThreadPool();//定时任务
//      Executors.newCachedThreadPool();//可缓存的线程池
//      Executors.newSingleThreadExecutor();//单个线程的线程池

        Executor e = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());

//      Executors.newWorkStealingPool();工作窃取
        ExecutorService executorService = Executors.newFixedThreadPool(5); //通过线程池限制线程创建数码
        for (int i = 0; i < 20; i++) {
            executorService.execute(new ThreadPoolDemo());
//            new Thread(new ThreadPoolDemo()).start();
        }
    }


    ArrayBlockingQueue blockingQueue = new ArrayBlockingQueue(20);

    public void run() {
        while (true) {
            try {
                blockingQueue.take();   //从阻塞队列获得数据，如果没有数据则阻塞队列。如阻塞队列添加数据之后则被唤醒（通知线程池而非线程池来监听阻塞队列）
                Object obj = blockingQueue.poll(1l, TimeUnit.SECONDS);    //线程回收，在等待时间到达阈值时还没有任务，可以认为当前线程池处于空闲状态，非核心线程可以回收。该方法就是设定等待时间
                if (obj == null){
                    break;//回收线程
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
