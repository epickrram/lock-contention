package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public final class SharedSocketSender implements Runnable
{
    static final String THREAD_NAME = "shared-socket-sender";
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8);
    private final long sendIntervalNanos;
    private final CountDownLatch latch = new CountDownLatch(1);

    public SharedSocketSender(final SocketChannel socketChannel, final long sendIntervalNanos) throws IOException
    {
        this.socketChannel = socketChannel;
        this.sendIntervalNanos = sendIntervalNanos;
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName(THREAD_NAME);
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
                System.err.println("Caught exception sending to server. Exiting.");
                e.printStackTrace();
            }
        }
    }

    void await() throws InterruptedException
    {
        latch.await();
    }
}
