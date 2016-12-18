package com.epickrram.sync;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class SynchronisationThroughput
{
    public static void main(final String[] args) throws Exception
    {
        final TimeUnit unit = TimeUnit.SECONDS;
        final long sleepTime = 15L;
        final int readerCount = 1;

        final Result syncReaderResult = runExchangerThroughputTest(new SyncExchanger(), unit, sleepTime, readerCount);
        final Result lockReaderResult = runExchangerThroughputTest(new LockExchanger(), unit, sleepTime, readerCount);
        final Result atomicReaderResult = runExchangerThroughputTest(new AtomicExchanger(), unit, sleepTime, readerCount);
        final Result volatileReaderResult = runExchangerThroughputTest(new VolatileExchanger(), unit, sleepTime, readerCount);
        final Result lazyReaderResult = runExchangerThroughputTest(new LazyExchanger(), unit, sleepTime, readerCount);
        final Result storeFenceResult = runExchangerThroughputTest(new StoreFenceExchanger(), unit, sleepTime, readerCount);

        System.out.println();
        reportTest("j.u.c.Lock   ", lockReaderResult);
        reportTest("sync         ", syncReaderResult);
        reportTest("volatile     ", volatileReaderResult);
        reportTest("atomic       ", atomicReaderResult);
        reportTest("lazySet      ", lazyReaderResult);
        reportTest("sfence       ", storeFenceResult);
    }

    private static void reportTest(final String testName, final Result result)
    {
        final long totalReads = result.distinctUpdateCount + result.noUpdateCount;
        System.out.printf("%sthrpt: %12d, reads: %12d, updates: %12d (%2d%%), noUpdates: %12d (%2d%%)%n",
                testName, result.readerValue, totalReads,
                result.distinctUpdateCount, (int) ((100 * result.distinctUpdateCount) / totalReads),
                result.noUpdateCount, (int) ((100 * result.noUpdateCount) / totalReads));
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
                                                     final TimeUnit unit, final long sleepTime,
                                                     final int readerThreadCount) throws InterruptedException
    {
        System.out.printf("Starting test with %s for %ds%n", exchanger.getClass().getSimpleName(), sleepTime);
        final Reader reader = new Reader(exchanger);
        final Thread rThread = new Thread(reader::run);
        rThread.start();
        final Thread wThread = new Thread(new Writer(exchanger)::run);
        wThread.start();
        for(int i = 0; i < readerThreadCount - 1; i++)
        {
            final Reader contendingReader = new Reader(exchanger);
            final Thread r = new Thread(contendingReader::run);
            r.setDaemon(true);
            r.start();
        }
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

}
