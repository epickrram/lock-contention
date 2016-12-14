package com.epickrram.lock.contention.socket;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class SingleThreadSocketExampleMain
{
    private static final int PORT = 14778;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final SingleSocketEchoServer echoServer = new SingleSocketEchoServer(PORT, 1);
        echoServer.start();

        final SocketChannel clientChannel = SocketChannel.open();
        clientChannel.connect(new InetSocketAddress("localhost", PORT));

        final SingleThreadSenderReceiver senderReceiver = new SingleThreadSenderReceiver(clientChannel, TimeUnit.MICROSECONDS.toNanos(200L), 2);
        executorService.submit(senderReceiver);

        senderReceiver.await();

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        final ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        long threadId = -1L;
        for (ThreadInfo threadInfo : threadInfos)
        {
            if(threadInfo.getThreadName().equals(SingleThreadSenderReceiver.THREAD_NAME))
            {
                threadId = threadInfo.getThreadId();
            }
        }

        if(threadId == -1L)
        {
            throw new IllegalStateException();
        }

        final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Times.TRIAL_SECONDS);
        while(System.currentTimeMillis() < endTime)
        {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        }

        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
        System.out.printf("[%s] blocked %d times for %dms%n", threadInfo.getThreadName(),
                threadInfo.getBlockedCount(), threadInfo.getBlockedTime());

        executorService.shutdownNow();
        echoServer.stop();

        executorService.awaitTermination(1L, TimeUnit.SECONDS);

        System.out.println(senderReceiver.getHistogramReporter().report("single thread receive latency"));
    }
}
