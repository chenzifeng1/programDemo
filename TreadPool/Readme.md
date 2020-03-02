# 线程池  
频繁的创建线程、销毁线程所带来的问题  
1. 线程创建不被限制，超出系统负载，会遇到服务挂掉等问题。
2. 线程数远大于CPU核心数之后会带来频繁的线程切换，每个线程都想抢占CPU，而线程的上下文切换会带来额外开销。（用户态到内核态的切换）  
 **应对措施：线程池化 提前创造好一定量的线程数，需要的时候实例化线程，用完后释放掉。**
 手动创建线程池使用 
 ```java
public class ThreadPoolExecutor{
     public ThreadPoolExecutor(int corePoolSize,    //核心线程数
                               int maximumPoolSize,  //最大线程数 
                               long keepAliveTime,   //线程存活时间
                               TimeUnit unit,    //线程存活时间单位
                               BlockingQueue<Runnable> workQueue,   //阻塞队列
                               ThreadFactory threadFactory,  //线程工厂
                               RejectedExecutionHandler handler) {  //拒绝策略
            this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                 Executors.defaultThreadFactory(), defaultHandler);
     }
}
```
参数含义：
- 核心线程数(corePoolSize):长驻线程池内的线程数，在线程池初始化时被实例化，知道线程池完结后释放。
- 最大线程数(maximumPoolSize):在任务繁忙时可临时创建线程来执行任务，临时线程+核心线程的最大数目。  
  在任务空闲时，临时线程被回收，留在线程池内的只有核心线程。
- 线程存活时间：在任务空闲时，临时线程在内存的存活时间
- 线程存活时间单位：存活时间的单位，在TimeUnit类中被定义。
