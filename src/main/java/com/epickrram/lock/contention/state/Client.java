package com.epickrram.lock.contention.state;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;

public final class Client
{
    private final int port;
    private Random random;
    private SocketChannel socketChannel;
    private long sumOfResults = 0L;

    public Client(final int port)
    {
        this.port = port;
    }

    public void request()
    {
        if(random == null)
        {
            random = new Random(System.nanoTime() ^ Thread.currentThread().getId());
        }

        if(socketChannel == null)
        {
            try
            {
                socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost(), port));
                socketChannel.configureBlocking(true);
            }
            catch(IOException e)
            {
                System.err.println("Couldn't connect to server");
            }
        }
        while(!Thread.currentThread().isInterrupted())
        {

            final long operand = random.nextLong();
            final int operation = random.nextInt() & 3;

            final ByteBuffer buffer = ByteBuffer.allocateDirect(12);
            buffer.putInt(operation);
            buffer.putLong(operand);
            buffer.flip();

            try
            {
                socketChannel.write(buffer);
                buffer.clear();
                socketChannel.read(buffer);
                buffer.flip();
                sumOfResults += buffer.getLong();
            }
            catch (Exception e)
            {
                // ignore
            }
            Thread.yield();
        }
    }

    public long getSumOfResults()
    {
        return sumOfResults;
    }
}
