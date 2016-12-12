package com.epickrram.lock.contention;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DuplexSocketEchoServer
{
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ServerSocketChannel serverSocketChannel;
    private final int port;
    private final int cpuAffinity;

    DuplexSocketEchoServer(final int port, final int cpuAffinity) throws IOException
    {
        this.cpuAffinity = cpuAffinity;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.port = port;
        this.serverSocketChannel.bind(new InetSocketAddress(this.port));
        this.serverSocketChannel.configureBlocking(true);
    }

    public void start()
    {
        executorService.submit(this::acceptLoop);
    }

    public void stop()
    {
        executorService.shutdownNow();
    }

    private void acceptLoop()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                final SocketChannel receiverChannel = serverSocketChannel.accept();
                final SocketChannel senderChannel = SocketChannel.open();
                senderChannel.connect(new InetSocketAddress("localhost", port + 1));
                senderChannel.configureBlocking(false);
                executorService.submit(new ClientConnectionHandler(receiverChannel, senderChannel, cpuAffinity));
            }
            catch (IOException e)
            {
                if(!(e instanceof ClosedByInterruptException  || e instanceof ClosedChannelException))
                {
                    System.err.println("Caught exception while accepting socket. Exiting.");
                    e.printStackTrace();
                }
            }
        }
    }
}
