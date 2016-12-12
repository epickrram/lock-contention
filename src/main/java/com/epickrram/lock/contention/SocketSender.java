package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public final class SocketSender implements Runnable
{
    static final String THREAD_NAME = "shared-socket-sender";
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8);
    private final long sendIntervalNanos;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final int cpuAffinity;

    public SocketSender(final SocketChannel socketChannel, final long sendIntervalNanos, final int cpuAffinity) throws IOException
    {
        this.socketChannel = socketChannel;
        this.sendIntervalNanos = sendIntervalNanos;
        this.cpuAffinity = cpuAffinity;
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
        CpuAffinity.setAffinity(cpuAffinity);
        latch.countDown();
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                buffer.putLong(System.nanoTime());
                buffer.flip();
                while(buffer.remaining() != 0)
                {
                    socketChannel.write(buffer);
                }

                LockSupport.parkNanos(sendIntervalNanos);
            }
            catch(IOException e)
            {
                if(!(e instanceof ClosedByInterruptException || e instanceof ClosedChannelException))
                {
                    System.err.println("Caught exception sending to server. Exiting.");
                    e.printStackTrace();
                }
            }
        }
    }

    void await() throws InterruptedException
    {
        latch.await();
    }
}
