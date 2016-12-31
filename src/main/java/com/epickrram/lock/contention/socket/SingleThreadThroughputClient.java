package com.epickrram.lock.contention.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class SingleThreadThroughputClient implements Runnable
{
    static final String THREAD_NAME = "single-thread-sender-receiver";
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final int cpuAffinity;
    private final HistogramReporter histogramReporter = new HistogramReporter();
    private long lastSecond = 0L;

    SingleThreadThroughputClient(final SocketChannel socketChannel, final int cpuAffinity) throws IOException
    {
        this.socketChannel = socketChannel;
        this.cpuAffinity = cpuAffinity;
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
        CpuAffinity.setAffinity(cpuAffinity);
        latch.countDown();
        long msgPerSecond = 0L;
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                buffer.putLong(17L);
                buffer.flip();
                while(buffer.remaining() != 0)
                {
                    socketChannel.write(buffer);
                }
                buffer.clear();
                while(buffer.remaining() != 0)
                {
                    socketChannel.read(buffer);
                }
                buffer.flip();
                msgPerSecond++;
                final long currentNanos = System.nanoTime();
                final long currentSecond = TimeUnit.NANOSECONDS.toSeconds(currentNanos);
                if(currentSecond != lastSecond)
                {
                    lastSecond = currentSecond;
                    System.out.printf("%d/sec%n", msgPerSecond);
                    msgPerSecond = 0L;
                }
            }
            catch (IOException e)
            {
                System.err.println("Caught exception while sending to/receiving from server. Exiting.");
                e.printStackTrace();
            }
        }
    }

    void await() throws InterruptedException
    {
        latch.await();
    }

    HistogramReporter getHistogramReporter()
    {
        return histogramReporter;
    }
}
