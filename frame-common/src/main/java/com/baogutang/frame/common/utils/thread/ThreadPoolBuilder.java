package com.baogutang.frame.common.utils.thread;

import org.apache.commons.lang3.Validate;

import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * ThreadPool创建的工具类.
 * <p>
 * 对比JDK Executors中的newFixedThreadPool(), newCachedThreadPool(),newScheduledThreadPool, 提供更多有用的配置项.
 * *
 * 使用示例如下：
 * ExecutorService ExecutorService = new FixedThreadPoolBuilder().setPoolSize(10).build();
 * @author N1KO
 */
public class ThreadPoolBuilder {

    private static final RejectedExecutionHandler defaultRejectHandler = new AbortPolicy();

    /**
     * @see FixedThreadPoolBuilder
     */
    public static FixedThreadPoolBuilder fixedPool() {
        return new FixedThreadPoolBuilder();
    }

    /**
     * @see CachedThreadPoolBuilder
     */
    public static CachedThreadPoolBuilder cachedPool() {
        return new CachedThreadPoolBuilder();
    }

    /**
     * @see ScheduledThreadPoolBuilder
     */
    public static ScheduledThreadPoolBuilder scheduledPool() {
        return new ScheduledThreadPoolBuilder();
    }

    /**
     * 创建FixedThreadPool.建议必须设置queueSize保证有界。
     * <p>
     * 1. 任务提交时, 如果线程数还没达到poolSize即创建新线程并绑定任务(即poolSize次提交后线程总数必达到poolSize，不会重用之前的线程)
     * <p>
     * poolSize默认为1，即singleThreadPool.
     * <p>
     * 2. 第poolSize次任务提交后, 新增任务放入Queue中, Pool中的所有线程从Queue中take任务执行.
     * <p>
     * Queue默认为无限长的LinkedBlockingQueue, 但建议设置queueSize换成有界的队列.
     * <p>
     * 如果使用有界队列, 当队列满了之后,会调用RejectHandler进行处理, 默认为AbortPolicy，抛出RejectedExecutionException异常.
     * 其他可选的Policy包括静默放弃当前任务(Discard)，放弃Queue里最老的任务(DisacardOldest)，或由主线程来直接执行(CallerRuns).
     * <p>
     * 3. 因为线程全部为core线程，所以不会在空闲时回收.
     */
    public static class FixedThreadPoolBuilder {

        private int poolSize = 1;
        private int queueSize = -1;

        private ThreadFactory threadFactory;
        private String threadNamePrefix;
        private Boolean daemon;

        private RejectedExecutionHandler rejectHandler;

        /**
         * Pool大小，默认为1，即singleThreadPool
         */
        public FixedThreadPoolBuilder setPoolSize(int poolSize) {
            Validate.isTrue(poolSize >= 1);
            this.poolSize = poolSize;
            return this;
        }

        /**
         * 不设置时默认为-1, 使用无限长的LinkedBlockingQueue.
         * <p>
         * 为正数时使用ArrayBlockingQueue.
         */
        public FixedThreadPoolBuilder setQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        /**
         * 与threadNamePrefix互斥, 优先使用ThreadFactory
         */
        public FixedThreadPoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        /**
         * 与ThreadFactory互斥, 优先使用ThreadFactory
         */
        public FixedThreadPoolBuilder setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        /**
         * 与threadFactory互斥, 优先使用ThreadFactory
         * <p>
         * 默认为NULL，不进行设置，使用JDK的默认值.
         */
        public FixedThreadPoolBuilder setDaemon(Boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public FixedThreadPoolBuilder setRejectHandler(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public ThreadPoolExecutor build() {
            BlockingQueue<Runnable> queue = null;
            if (queueSize < 1) {
                queue = new LinkedBlockingQueue<Runnable>();
            } else {
                queue = new ArrayBlockingQueue<Runnable>(queueSize);
            }

            threadFactory = createThreadFactory(threadFactory, threadNamePrefix, daemon);

            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }

            return new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, queue, threadFactory,
                    rejectHandler);
        }
    }

    /**
     * 创建CachedThreadPool, maxSize建议设置
     * <p>
     * 1. 任务提交时, 如果线程数还没达到minSize即创建新线程并绑定任务(即minSize次提交后线程总数必达到minSize, 不会重用之前的线程)
     * <p>
     * minSize默认为0, 可设置保证有基本的线程处理请求不被回收.
     * <p>
     * 2. 第minSize次任务提交后, 新增任务提交进SynchronousQueue后，如果没有空闲线程立刻处理，则会创建新的线程, 直到总线程数达到上限.
     * <p>
     * maxSize默认为Integer.Max, 可以进行设置.
     * <p>
     * 如果设置了maxSize, 当总线程数达到上限, 会调用RejectHandler进行处理, 默认为AbortPolicy, 抛出RejectedExecutionException异常.
     * 其他可选的Policy包括静默放弃当前任务(Discard)，或由主线程来直接执行(CallerRuns).
     * <p>
     * 3. minSize以上, maxSize以下的线程, 如果在keepAliveTime中都poll不到任务执行将会被结束掉, keeAliveTimeJDK默认为10秒.
     * JDK默认值60秒太高，如高达1000线程时，要低于16QPS时才会开始回收线程, 因此改为默认10秒.
     */
    public static class CachedThreadPoolBuilder {

        private int minSize = 0;
        private int maxSize = Integer.MAX_VALUE;
        private int keepAliveSecs = 10;

        private ThreadFactory threadFactory;
        private String threadNamePrefix;
        private Boolean daemon;

        private RejectedExecutionHandler rejectHandler;

        public CachedThreadPoolBuilder setMinSize(int minSize) {
            this.minSize = minSize;
            return this;
        }

        /**
         * Max默认Integer.MAX_VALUE的，建议设置
         */
        public CachedThreadPoolBuilder setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        /**
         * JDK默认值60秒太高，如高达1000线程时，要低于16QPS时才会开始回收线程, 因此改为默认10秒.
         */
        public CachedThreadPoolBuilder setKeepAliveSecs(int keepAliveSecs) {
            this.keepAliveSecs = keepAliveSecs;
            return this;
        }

        /**
         * 与threadNamePrefix互斥, 优先使用ThreadFactory
         */
        public CachedThreadPoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        /**
         * 与threadFactory互斥, 优先使用ThreadFactory
         */
        public CachedThreadPoolBuilder setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        /**
         * 与threadFactory互斥, 优先使用ThreadFactory
         * <p>
         * 默认为NULL，不进行设置，使用JDK的默认值.
         */
        public CachedThreadPoolBuilder setDaemon(Boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public CachedThreadPoolBuilder setRejectHanlder(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public ThreadPoolExecutor build() {

            threadFactory = createThreadFactory(threadFactory, threadNamePrefix, daemon);

            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }

            return new ThreadPoolExecutor(minSize, maxSize, keepAliveSecs, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), threadFactory, rejectHandler);
        }
    }

    /**
     * 创建ScheduledPool
     */
    public static class ScheduledThreadPoolBuilder {

        private int poolSize = 1;
        private ThreadFactory threadFactory;
        private String threadNamePrefix;

        /**
         * 默认为1
         */
        public ScheduledThreadPoolBuilder setPoolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        /**
         * 与threadNamePrefix互斥, 优先使用ThreadFactory
         */
        public ScheduledThreadPoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public ScheduledThreadPoolBuilder setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public ScheduledThreadPoolExecutor build() {
            threadFactory = createThreadFactory(threadFactory, threadNamePrefix, Boolean.TRUE);
            return new ScheduledThreadPoolExecutor(poolSize, threadFactory);
        }
    }

    /**
     * 优先使用threadFactory，否则如果threadNamePrefix不为空则使用自建ThreadFactory，否则使用defaultThreadFactory
     */
    private static ThreadFactory createThreadFactory(ThreadFactory threadFactory, String threadNamePrefix,
                                                     Boolean daemon) {
        if (threadFactory != null) {
            return threadFactory;
        }

        if (threadNamePrefix != null) {
            if (daemon != null) {
                return ThreadPoolUtil.buildThreadFactory(threadNamePrefix, daemon);
            } else {
                return ThreadPoolUtil.buildThreadFactory(threadNamePrefix);
            }
        }

        return Executors.defaultThreadFactory();
    }
}
