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

        Executor e = new ThreadPoolExecutor(0,Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());

//      Executors.newWorkStealingPool();工作窃取
        ExecutorService executorService = Executors.newFixedThreadPool(5); //通过线程池限制线程创建数码
        for (int i = 0; i < 20; i++) {
            executorService.execute(new ThreadPoolDemo());
//            new Thread(new ThreadPoolDemo()).start();
        }
    }

    public void run() {
        System.out.println("name:" + Thread.currentThread().getName());
    }
}
