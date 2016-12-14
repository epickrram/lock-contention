package com.epickrram.lock.contention.state;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CommandServer
{
    private final ExecutorService executor =
            Executors.newCachedThreadPool();
    private final int port;
    private final MutableState mutableState;

    public CommandServer(final int port, final MutableState mutableState)
    {
        this.port = port;
        this.mutableState = mutableState;
    }

    public void start()
    {
        executor.execute(this::acceptLoop);
    }

    public void stop()
    {
        executor.shutdownNow();
    }

    private void acceptLoop()
    {
        try
        {
            final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(true);
            while(!Thread.currentThread().isInterrupted())
            {
                final SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                executor.submit(new ClientHandler(socketChannel, mutableState)::processRequests);
            }
        }
        catch (IOException e)
        {
            System.err.println("Accept loop exiting");
        }
    }
}
