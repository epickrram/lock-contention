package com.epickrram.sync;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class SyncThroughputBenchmark
{
    public enum ExchangerType
    {
        ATOMIC,
        LOCK,
        SYNC,
        LAZY,
        VOLATILE,
        FENCE
    }

    @Param
    private ExchangerType exchangerType;
    private Exchanger exchanger;
    public Reader reader;
    private long counter;
    private Thread rThread;

    @Setup
    public void setup()
    {
        switch(exchangerType)
        {
            case ATOMIC:
                exchanger = new AtomicExchanger();
                break;
            case LOCK:
                exchanger = new LockExchanger();
                break;
            case SYNC:
                exchanger = new SyncExchanger();
                break;
            case LAZY:
                exchanger = new LazyExchanger();
                break;
            case VOLATILE:
                exchanger = new VolatileExchanger();
                break;
            case FENCE:
                exchanger = new StoreFenceExchanger();
                break;
            default:
                throw new IllegalArgumentException();
        }

        reader = new Reader(exchanger);
        rThread = new Thread(reader::burnCpu);
        rThread.setDaemon(true);
        rThread.start();
    }

    @TearDown
    public void teardown() throws InterruptedException
    {
        if(rThread != null)
        {
            rThread.interrupt();
            rThread.join();
        }
    }

    @Benchmark
    public long update()
    {
        exchanger.updateCounter(counter++);
        return counter;
    }

    public static final class Reader
    {
        public static long total;

        private final Exchanger exchanger;

        public Reader(final Exchanger exchanger)
        {
            this.exchanger = exchanger;
        }

        public void burnCpu()
        {
            while(!Thread.currentThread().isInterrupted())
            {
                total += exchanger.getCounter();
            }
        }
    }
}
