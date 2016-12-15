import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public final class SynchronisationThroughput
{
    public static void main(final String[] args) throws Exception
    {
        final TimeUnit unit = TimeUnit.SECONDS;
        final long sleepTime = 15L;

        final Result lazyReaderResult = runExchangerThroughputTest(new LazyExchanger(), unit, sleepTime);
        final Result syncReaderResult = runExchangerThroughputTest(new SyncExchanger(), unit, sleepTime);
        final Result lockReaderResult = runExchangerThroughputTest(new LockExchanger(), unit, sleepTime);
        final Result atomicReaderResult = runExchangerThroughputTest(new AtomicExchanger(), unit, sleepTime);

        System.out.println();
        System.out.printf("j.u.c.Lock thrpt: %12d, updates: %12d, noUpdates: %12d%n",
                lockReaderResult.readerValue, lockReaderResult.distinctUpdateCount, lockReaderResult.noUpdateCount);
        System.out.printf("sync       thrpt: %12d, updates: %12d, noUpdates: %12d%n",
                syncReaderResult.readerValue, syncReaderResult.distinctUpdateCount, syncReaderResult.noUpdateCount);
        System.out.printf("atomic     thrpt: %12d, updates: %12d, noUpdates: %12d%n",
                atomicReaderResult.readerValue, atomicReaderResult.distinctUpdateCount, atomicReaderResult.noUpdateCount);
        System.out.printf("lazySet    thrpt: %12d, updates: %12d, noUpdates: %12d%n",
                lazyReaderResult.readerValue, lazyReaderResult.distinctUpdateCount, lazyReaderResult.noUpdateCount);

        System.out.println();
        System.out.printf("Lazy exchanger has ~%.2f times higher throughput than atomic%n", lazyReaderResult.readerValue / (double) atomicReaderResult.readerValue);
        System.out.printf("Lazy exchanger has ~%.2f times higher throughput than synchronized%n", lazyReaderResult.readerValue / (double) syncReaderResult.readerValue);
        System.out.printf("Lazy exchanger has ~%.2f times higher throughput than j.u.c.Lock%n", lazyReaderResult.readerValue / (double) lockReaderResult.readerValue);
    }

    private static final class Result
    {
        private final long readerValue;
        private final long distinctUpdateCount;
        private final long noUpdateCount;

        private Result(final long readerValue, final long distinctUpdateCount, final long noUpdateCount)
        {
            this.readerValue = readerValue;
            this.distinctUpdateCount = distinctUpdateCount;
            this.noUpdateCount = noUpdateCount;
        }
    }

    private static Result runExchangerThroughputTest(final Exchanger exchanger,
                                                     final TimeUnit unit, final long sleepTime) throws InterruptedException
    {
        System.out.printf("Starting test with %s for %ds%n", exchanger.getClass().getSimpleName(), sleepTime);
        final Reader reader = new Reader(exchanger);
        final Thread rThread = new Thread(reader::run);
        rThread.start();
        final Thread wThread = new Thread(new Writer(exchanger)::run);
        wThread.start();
        final long writerThreadId = wThread.getId();
        final long readerThreadId = rThread.getId();
        final long[] ids = new long[] {readerThreadId, writerThreadId};
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        threadMXBean.setThreadCpuTimeEnabled(true);

        LockSupport.parkNanos(unit.toNanos(sleepTime));

        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ids);
        for (ThreadInfo threadInfo : threadInfos)
        {
            System.out.printf("%s blocked %dms (%d), waited %dms (%d)%n",
                    threadInfo.getThreadId() == writerThreadId ? "writer" : "reader",
                    threadInfo.getBlockedTime(), threadInfo.getBlockedCount(),
                    threadInfo.getWaitedTime(), threadInfo.getWaitedCount());
        }

        rThread.interrupt();
        wThread.interrupt();
        rThread.join();
        wThread.join();

        System.out.println();

        return new Result(reader.getValue(), reader.getDistinctUpdateCount(), reader.getNoUpdateCount());
    }

    private static final class Writer
    {
        private final Exchanger exchanger;
        private long value;

        private Writer(final Exchanger exchanger)
        {
            this.exchanger = exchanger;
        }

        public void run()
        {
            while(!Thread.currentThread().isInterrupted())
            {
                exchanger.update(value++);
            }
        }
    }

    private static final class Reader
    {
        private final Exchanger exchanger;
        private long value;
        private long distinctUpdateCount = 0L;
        private long noUpdateCount = 0L;

        private Reader(final Exchanger exchanger)
        {
            this.exchanger = exchanger;
        }

        public void run()
        {
            while(!Thread.currentThread().isInterrupted())
            {
                final long current = exchanger.get();
                if(current != value)
                {
                    distinctUpdateCount++;
                }
                else
                {
                    noUpdateCount++;
                }
                value = current;
            }
        }

        long getValue()
        {
            return value;
        }

        long getDistinctUpdateCount()
        {
            return distinctUpdateCount;
        }

        long getNoUpdateCount()
        {
            return noUpdateCount;
        }
    }

    interface Exchanger
    {
        void update(final long v);
        long get();
    }

    private static final class LockExchanger implements Exchanger
    {
        private final Lock lock = new ReentrantLock();
        private long value;

        @Override
        public void update(final long v)
        {
            lock.lock();
            try
            {
                value = v;
            }
            finally
            {
                lock.unlock();
            }
        }

        @Override
        public long get()
        {
            lock.lock();
            try
            {
                return value;
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    private static final class LazyExchanger implements Exchanger
    {
        private final AtomicLong value = new AtomicLong();

        @Override
        public void update(final long v)
        {
            value.lazySet(v);
        }

        @Override
        public long get()
        {
            return value.get();
        }
    }

    private static final class AtomicExchanger implements Exchanger
    {
        private final AtomicLong value = new AtomicLong();

        @Override
        public void update(final long v)
        {
            value.set(v);
        }

        @Override
        public long get()
        {
            return value.get();
        }
    }

    private static final class SyncExchanger implements Exchanger
    {
        private final Object lock = new Object();
        private long value;

        @Override
        public void update(final long v)
        {
            synchronized (lock)
            {
                value = v;
            }
        }

        @Override
        public long get()
        {
            synchronized (lock)
            {
                return value;
            }
        }
    }
}
