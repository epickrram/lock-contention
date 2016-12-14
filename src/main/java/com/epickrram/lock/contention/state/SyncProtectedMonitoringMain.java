package com.epickrram.lock.contention.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncProtectedMonitoringMain
{
    private static final int PORT = 14778;

    public static void main(final String[] args)
    {
        final CommandServer commandServer = new CommandServer(PORT, new SyncProtectedMutableState());
        commandServer.start();

        final ExecutorService threadPool = Executors.newCachedThreadPool();
        for(int i = 0; i < 10; i++)
        {
            threadPool.submit(new Client(PORT)::request);
        }

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new ContentionMonitor()::monitor, 1L, 1L, TimeUnit.SECONDS);
    }
}
