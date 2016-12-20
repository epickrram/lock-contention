package com.epickrram.sync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class TestRunner
{
    public static void main(String[] args) throws InterruptedException
    {
        final char testType = args[0].charAt(0);
        final Exchanger exchanger;
        switch(testType)
        {
            case 'A':
                exchanger = new AtomicExchanger();
                break;
            case 'L':
                exchanger = new LockExchanger();
                break;
            case 'F':
                exchanger = new StoreFenceExchanger();
                break;
            case 'S':
                exchanger = new SyncExchanger();
                break;
            case 'V':
                exchanger = new VolatileExchanger();
                break;
            case 'Z':
                exchanger = new LazyExchanger();
                break;
            default:
                throw new IllegalArgumentException();
        }

        runTest(exchanger);
    }

    private static void runTest(final Exchanger exchanger) throws InterruptedException
    {
        final Reader reader = new Reader(exchanger);
        final Thread rThread = new Thread(reader::run);
        rThread.start();
        final Thread wThread = new Thread(new Writer(exchanger)::run);
        wThread.setName("contention-test-writer");
        wThread.start();
        final Reader contendingReader = new Reader(exchanger);
        final Thread r = new Thread(contendingReader::run);
        r.setDaemon(true);
        r.start();

        LockSupport.parkNanos(TimeUnit.HOURS.toNanos(1L));

        rThread.interrupt();
        wThread.interrupt();
        rThread.join();
        wThread.join();
    }
}
