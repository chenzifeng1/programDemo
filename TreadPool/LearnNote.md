# 线程池  
频繁的创建线程、销毁线程所带来的问题  
1. 线程创建不被限制，超出系统负载，会遇到服务挂掉等问题。
2. 线程数远大于CPU核心数之后会带来频繁的线程切换，每个线程都想抢占CPU，而线程的上下文切换会带来额外开销。（用户态到内核态的切换）
3. 无需区分核心或者是非核心线程，因为线程本身是无状态的，只要保留线程的数量即可，即保证线程池中的线程数是核心线程的数量即即可。  
 **应对措施：线程池化 提前创造好一定量的线程数，需要的时候实例化线程，用完后释放掉。**
 
 Runnable是一个接口，Thread是实现该接口的类。
 
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
![线程池创建原理](/img/线程池原理图.PNG)  
问题：
- 如何判断线程池繁忙或空闲：看阻塞队列中的任务是否为空  
- 如何重复利用线程，因为线程run方法结束，则线程生命周期也就结束，唯一可以重复利用线程的方法，就是在线程内写循环。
在循环内部，我们可以利用阻塞队列来做到有任务时候线程执行，没任务的时候线程阻塞。  
线程池：创建一个线程池，所有客户端需要执行的任务都添加阻塞队列里面，线程池中来轮询阻塞队列中的任务。
- 回收非核心线程：可以设定阻塞时间，当阻塞队列一定时间内都没有任务进来，可以认为线程池处于了空闲状态，可以回收非核心线程来减少资源占用。回收线程只要break掉循环即可，因为run()方法结束之后线程自动被回收。
- 是否是核心线程，取决于参数

```java
public class ThreadPoolExecutor{

    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0)); //ctl标识线程池状态信息，是一个包含两个概念的原子整数
    private static final int COUNT_BITS = Integer.SIZE - 3; //计数位数，32-3=29位
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1; //00011111111111111111111111111110

    // runState is stored in the high-order bits    状态信息的二进制信息
    private static final int RUNNING    = -1 << COUNT_BITS; //11100000000000000000000000000000
    private static final int SHUTDOWN   =  0 << COUNT_BITS; //00000000000000000000000000000000
    private static final int STOP       =  1 << COUNT_BITS; //00100000000000000000000000000000
    private static final int TIDYING    =  2 << COUNT_BITS; //01000000000000000000000000000000 
    private static final int TERMINATED =  3 << COUNT_BITS; //01100000000000000000000000000000

    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps: 线程池执行过程分三步
         * 1. 如果核心线程较少（少于设定的核心线程数参数的值），则初始化一个核心线程来执行命令作为它的第一个任务。
         * 此次调用会自动检查运行状态及工作线程数来避免错误状态
         * 
         * 2. 如果一个任务进入阻塞队列，之后我们依然需要二次检查我们是否需要添加一个线程
         * 任务进入阻塞队列，一般说明核心线程数已到达最大值，需要建立非核心线程来执行任务???
         * 线程创建后从阻塞队列获取任务来执行，线程池预热结束。当然，线程池可以提前预热，即在初始化时就创建一定量的核心线程数。
         *
         * 3. 如果我们无法将任务入队，说明阻塞队列已满或者是线程池已经关闭
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {  
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {    //将任务添加到队列
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command);
        }
        
    
    private boolean addWorker(Runnable firstTask, boolean core) {
            retry:
            for (;;) {
                int c = ctl.get();  //获取ctl
                int rs = runStateOf(c); //获取运行状态
    
                // Check if queue empty only if necessary.
                if (rs >= SHUTDOWN &&   
                    ! (rs == SHUTDOWN &&
                       firstTask == null &&
                       ! workQueue.isEmpty()))
                    return false;
    
                for (;;) {
                    int wc = workerCountOf(c);  //计数
                    if (wc >= CAPACITY ||
                        wc >= (core ? corePoolSize : maximumPoolSize))
                        return false;
                    if (compareAndIncrementWorkerCount(c))
                        break retry;
                    c = ctl.get();  // Re-read ctl
                    if (runStateOf(c) != rs)
                        continue retry;
                    // else CAS failed due to workerCount change; retry inner loop
                }
            }
            //上述循环来对worker进行计数
            //下列的代码是创建一个新的worker
            boolean workerStarted = false;
            boolean workerAdded = false;
            Worker w = null;
            try {
                w = new Worker(firstTask);  //创建worker，worker是一个实现runnable的类，可以认为是一个线程
                final Thread t = w.thread;  //Worker类内部有一个thread对象
                if (t != null) {
                    final ReentrantLock mainLock = this.mainLock;   
                    mainLock.lock();    //加锁
                    try {
                        // Recheck while holding lock.
                        // Back out on ThreadFactory failure or if
                        // shut down before lock acquired.
                        int rs = runStateOf(ctl.get()); //得到状态
    
                        if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                            if (t.isAlive()) // precheck that t is startable
                                throw new IllegalThreadStateException();
                            workers.add(w);
                            int s = workers.size();
                            if (s > largestPoolSize)
                                largestPoolSize = s;
                            workerAdded = true;
                        }
                    } finally {
                        mainLock.unlock();
                    }
                    if (workerAdded) {
                        t.start();
                        workerStarted = true;
                    }
                }
            } finally {
                if (! workerStarted)
                    addWorkerFailed(w);
            }
            return workerStarted;
        }
}
```
### 源码分析
ctl是一个32位整数，其中包含两个信息：  
workerCount：有效线程数  
runState：线程运行状态  
为了将两个信息放在一个int变量里面，规定低29位来标识workerCount，高3位来标识runState
  
addWorker(Runnable firstTask, boolean core) 
- 创建线程（是否为核心线程取决与参数 core）
- 启动线程
- 判断线程是否超过最大数量

