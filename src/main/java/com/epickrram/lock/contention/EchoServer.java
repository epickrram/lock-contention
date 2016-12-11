package com.epickrram.lock.contention;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EchoServer
{
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ServerSocketChannel serverSocketChannel;

    public EchoServer(final int port) throws IOException
    {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
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
                final SocketChannel client = serverSocketChannel.accept();
                executorService.submit(new ClientConnectionHandler(client));
            }
            catch (IOException e)
            {
                System.err.println("Caught exception while accepting socket. Exiting.");
                e.printStackTrace();
            }
        }
    }
}
