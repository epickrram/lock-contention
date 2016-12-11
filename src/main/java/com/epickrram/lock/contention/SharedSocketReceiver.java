package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class SharedSocketReceiver implements Runnable
{
    static final String THREAD_NAME = "shared-socket-receiver";
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;
    private final CountDownLatch latch = new CountDownLatch(1);
    private long maxInSecond = 0L;
    private long lastSecond = 0L;

    SharedSocketReceiver(final SocketChannel socketChannel) throws IOException
    {
        this.socketChannel = socketChannel;
        this.buffer = ByteBuffer.allocateDirect(8);
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
        latch.countDown();
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
            }
            catch(IOException e)
            {
                System.err.println("Caught exception while receiving from server. Exiting.");
                e.printStackTrace();
            }
        }
    }

    void await() throws InterruptedException
    {
        latch.await();
    }
}
