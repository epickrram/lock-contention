package com.epickrram.lock.contention.state;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientHandler
{
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private final SocketChannel socketChannel;
    private final MutableState mutableState;
    private final ByteBuffer requestBuffer = ByteBuffer.allocateDirect(12);
    private final ByteBuffer responseBuffer = ByteBuffer.allocateDirect(8);

    ClientHandler(final SocketChannel socketChannel, final MutableState mutableState)
    {
        this.socketChannel = socketChannel;
        this.mutableState = mutableState;
    }

    public void processRequests()
    {
        Thread.currentThread().setName("client-handler-" + THREAD_ID.getAndIncrement());
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                requestBuffer.clear();
                while (requestBuffer.remaining() != 0)
                {
                    Thread.yield();
                    socketChannel.read(requestBuffer);
                }
                requestBuffer.flip();
                final int opId = requestBuffer.getInt();
                final long operand = requestBuffer.getLong();
                final Command.Operation operation = Command.Operation.values()[opId];
                final Command command = new Command(operand, operation);

                long result = Long.MIN_VALUE;
                switch(operation)
                {
                    case ADD:
                    case SUBTRACT:
                        result = mutableState.mutate(command);
                        break;
                    case GET_CURRENT:
                        result = mutableState.getValue();
                        break;
                    case GET_HISTORY:
                        result = mutableState.getHistoric((int) operand);
                        break;
                }

                responseBuffer.clear();
                responseBuffer.putLong(result);
                responseBuffer.flip();
                while(responseBuffer.remaining() != 0)
                {
                    socketChannel.write(responseBuffer);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
                // ignore
            }
        }
    }
}
