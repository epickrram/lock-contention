package com.epickrram.lock.contention;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

final class ClientConnectionHandler implements Runnable
{
    private final SocketChannel receiverChannel;
    private final SocketChannel senderChannel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8);

    ClientConnectionHandler(final SocketChannel receiverChannel, final SocketChannel senderChannel) throws IOException
    {
        this.receiverChannel = receiverChannel;
        this.senderChannel = senderChannel;
        receiverChannel.configureBlocking(false);
        senderChannel.configureBlocking(false);
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("socket-connection-handler");
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                buffer.clear();
                while (buffer.remaining() != 0)
                {
                    receiverChannel.read(buffer);
                }
                buffer.flip();
                while(buffer.remaining() != 0)
                {
                    senderChannel.write(buffer);
                }
            }
            catch(IOException e)
            {
                System.err.println("Caught exception while reading from client. Exiting.");
                e.printStackTrace();
            }
        }
    }
}