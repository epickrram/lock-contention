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
    public static void main(final String[] args)
    {
        final TimeUnit unit = TimeUnit.MINUTES;
        final long sleepTime = 1L;

        final long lazyReaderValue = runExchangerThroughputTest(new LazyExchanger(), unit, sleepTime);
        final long syncReaderValue = runExchangerThroughputTest(new SyncExchanger(), unit, sleepTime);
        final long lockReaderValue = runExchangerThroughputTest(new LockExchanger(), unit, sleepTime);

        System.out.printf("%n%nj.u.c.Lock: %d, synchronized: %d, async: %d%n", lockReaderValue, syncReaderValue, lazyReaderValue);
        System.out.printf("Lazy is %.2f times faster than synchronized%n", lazyReaderValue / (double) syncReaderValue);
        System.out.printf("Lazy is %.2f times faster than j.u.c.Lock%n", lazyReaderValue / (double) lockReaderValue);
    }

    private static long runExchangerThroughputTest(final Exchanger exchanger, final TimeUnit unit, final long sleepTime)
    {
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

        final Thread monitor = new Thread(() ->
                                         {
                                             while(!Thread.currentThread().isInterrupted())
                                             {
                                                 final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ids);
                                                 for (ThreadInfo threadInfo : threadInfos)
                                                 {
                                                     System.out.printf("%n%s blocked %dms (%d), waited %dms (%d)",
                                                                       threadInfo.getThreadId() == writerThreadId ? "writer" : "reader",
                                                                       threadInfo.getBlockedTime(), threadInfo.getBlockedCount(),
                                                                       threadInfo.getWaitedTime(), threadInfo.getWaitedCount());
                                                 }
                                                 LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
                                             }
                                         });
        monitor.start();

        LockSupport.parkNanos(unit.toNanos(sleepTime));

        final long readerValue = reader.getValue();

        rThread.interrupt();
        wThread.interrupt();
        monitor.interrupt();
        return readerValue;
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

        private Reader(final Exchanger exchanger)
        {
            this.exchanger = exchanger;
        }

        public void run()
        {
            while(!Thread.currentThread().isInterrupted())
            {
                value = exchanger.get();
            }
        }

        public long getValue()
        {
            return value;
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
