package com.epickrram.lock.contention.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.epickrram.lock.contention.socket.CpuAffinity.unSetAffinity;

public final class SingleThreadThroughputExampleMain
{
    private static final int PORT = 14778;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final SingleSocketEchoServer echoServer = new SingleSocketEchoServer(PORT, unSetAffinity());
        echoServer.start();

        final SocketChannel clientChannel = SocketChannel.open();
        clientChannel.connect(new InetSocketAddress("localhost", PORT));

        final SingleThreadThroughputClient senderReceiver =
                new SingleThreadThroughputClient(clientChannel, unSetAffinity());
        executorService.submit(senderReceiver);

        senderReceiver.await();

        final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Times.TRIAL_SECONDS);
        while(System.currentTimeMillis() < endTime)
        {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        }

        executorService.shutdownNow();
        echoServer.stop();

        executorService.awaitTermination(1L, TimeUnit.SECONDS);
    }
}
