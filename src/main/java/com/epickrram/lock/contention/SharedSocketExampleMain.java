package com.epickrram.lock.contention;

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

        final EchoServer echoServer = new EchoServer(PORT);
        echoServer.start();

        final SocketChannel clientChannel = SocketChannel.open();
        clientChannel.connect(new InetSocketAddress("localhost", PORT));

        final SharedSocketReceiver sharedSocketReceiver = new SharedSocketReceiver(clientChannel);
        final SharedSocketSender sharedSocketSender = new SharedSocketSender(clientChannel, TimeUnit.MICROSECONDS.toNanos(200L));
        executorService.submit(sharedSocketReceiver);
        executorService.submit(sharedSocketSender);

        sharedSocketReceiver.await();
        sharedSocketSender.await();

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        final ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        final long[] contendingThreads = new long[2];
        int ptr = 0;
        for (ThreadInfo threadInfo : threadInfos)
        {
            if(threadInfo.getThreadName().equals(SharedSocketReceiver.THREAD_NAME))
            {
                contendingThreads[ptr++] = threadInfo.getThreadId();
            }
            else if(threadInfo.getThreadName().equals(SharedSocketSender.THREAD_NAME))
            {
                contendingThreads[ptr++] = threadInfo.getThreadId();
            }
        }

        final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60L);
        while(System.currentTimeMillis() < endTime)
        {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));

            ThreadInfo threadInfo = threadMXBean.getThreadInfo(contendingThreads[0]);
            System.out.printf("[%s] blocked %d times for %dms%n", threadInfo.getThreadName(),
                    threadInfo.getBlockedCount(), threadInfo.getBlockedTime());
            threadInfo = threadMXBean.getThreadInfo(contendingThreads[1]);
            System.out.printf("[%s] blocked %d times for %dms%n", threadInfo.getThreadName(),
                    threadInfo.getBlockedCount(), threadInfo.getBlockedTime());
        }


        executorService.shutdownNow();
        echoServer.stop();
    }
}
