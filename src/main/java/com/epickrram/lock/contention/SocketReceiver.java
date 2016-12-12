package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class SocketReceiver implements Runnable
{
    static final String THREAD_NAME = "shared-socket-receiver";
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final int cpuAffinity;
    private final HistogramReporter histogramReporter = new HistogramReporter();
    private boolean resetData = true;

    //-XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:+PrintCompilation -XX:+PrintInlining -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCTaskTimeStamps -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -XX:+PrintGC -XX:+PrintSafepointStatistics -XX:PrintSafepointStatisticsCount=5 -XX:GuaranteedSafepointInterval=600000

    SocketReceiver(final SocketChannel socketChannel, final int cpuAffinity) throws IOException
    {
        this.socketChannel = socketChannel;
        this.cpuAffinity = cpuAffinity;
        this.buffer = ByteBuffer.allocateDirect(8);
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
        latch.countDown();
        CpuAffinity.setAffinity(cpuAffinity);
        final long recordingStartTimestamp = System.nanoTime() + TimeUnit.SECONDS.toNanos(Times.WARMUP_SECONDS);
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                while(buffer.remaining() != 0)
                {
                    socketChannel.read(buffer);
                }
                buffer.flip();
                final long currentNanos = System.nanoTime();
                if(currentNanos > recordingStartTimestamp && resetData)
                {
                    histogramReporter.reset();
                    resetData = false;
                }
                final long roundTripLatency = currentNanos - buffer.getLong();
                histogramReporter.recordValue(roundTripLatency);
            }
            catch(IOException e)
            {
                if(!(e instanceof ClosedByInterruptException || e instanceof ClosedChannelException))
                {
                    System.err.println("Caught exception while receiving from server. Exiting.");
                    e.printStackTrace();
                }
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
