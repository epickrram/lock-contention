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

public final class SharedSocketExampleMain
{
    private static final int PORT = 14778;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final SingleSocketEchoServer echoServer = new SingleSocketEchoServer(PORT, 1);
        echoServer.start();

        final SocketChannel clientChannel = SocketChannel.open();
        clientChannel.connect(new InetSocketAddress("localhost", PORT));

        final SocketReceiver socketReceiver = new SocketReceiver(clientChannel, 2);
        final SocketSender socketSender = new SocketSender(clientChannel, TimeUnit.MICROSECONDS.toNanos(200L), 3);
        executorService.submit(socketReceiver);
        executorService.submit(socketSender);

        socketReceiver.await();
        socketSender.await();

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        final ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        final long[] contendingThreads = new long[2];
        int ptr = 0;
        for (ThreadInfo threadInfo : threadInfos)
        {
            if(threadInfo.getThreadName().equals(SocketReceiver.THREAD_NAME))
            {
                contendingThreads[ptr++] = threadInfo.getThreadId();
            }
            else if(threadInfo.getThreadName().equals(SocketSender.THREAD_NAME))
            {
                contendingThreads[ptr++] = threadInfo.getThreadId();
            }
        }

        final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Times.TRIAL_SECONDS);
        while(System.currentTimeMillis() < endTime)
        {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        }

        ThreadInfo threadInfo = threadMXBean.getThreadInfo(contendingThreads[0]);
        System.out.printf("[%s] blocked %d times for %dms%n", threadInfo.getThreadName(),
                threadInfo.getBlockedCount(), threadInfo.getBlockedTime());
        threadInfo = threadMXBean.getThreadInfo(contendingThreads[1]);
        System.out.printf("[%s] blocked %d times for %dms%n", threadInfo.getThreadName(),
                threadInfo.getBlockedCount(), threadInfo.getBlockedTime());

        executorService.shutdownNow();
        echoServer.stop();

        executorService.awaitTermination(1L, TimeUnit.SECONDS);

        System.out.println(socketReceiver.getHistogramReporter().report("shared receive latency"));
    }
}
