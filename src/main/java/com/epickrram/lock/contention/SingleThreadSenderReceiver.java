package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class SingleThreadSenderReceiver implements Runnable
{
    static final String THREAD_NAME = "single-thread-sender-receiver";
    private final SocketChannel socketChannel;
    private final long sendIntervalNanos;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8);
    private final CountDownLatch latch = new CountDownLatch(1);
    private long maxInSecond = 0L;
    private long lastSecond = 0L;

    SingleThreadSenderReceiver(final SocketChannel socketChannel, final long sendIntervalNanos) throws IOException
    {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.sendIntervalNanos = sendIntervalNanos;
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
        latch.countDown();
        final long recordingStartTimestamp = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                final long startTime = System.nanoTime();
                buffer.putLong(startTime);
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
                final long currentNanos = System.nanoTime();
                if(currentNanos < recordingStartTimestamp)
                {
                    continue;
                }
                final long roundTripLatency = currentNanos - buffer.getLong();
                final long currentSecond = TimeUnit.NANOSECONDS.toSeconds(currentNanos);
                maxInSecond = Math.max(maxInSecond, roundTripLatency);
                if(currentSecond != lastSecond)
                {
                    if(lastSecond != 0)
                    {
                        System.out.println(roundTripLatency);
                    }

                    lastSecond = currentSecond;
                    maxInSecond = 0L;
                }
                LockSupport.parkNanos(sendIntervalNanos - roundTripLatency);
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
}
