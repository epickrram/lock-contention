package com.epickrram.sync;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class LockExchanger extends AbstractExchanger
{
    private final Lock lock = new ReentrantLock();
    private long value;

    @Override
    public void updateCounter(final long v)
    {
        lock.lock();
        try
        {
            value = v;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public long getCounter()
    {
        lock.lock();
        try
        {
            return value;
        }
        finally
        {
            lock.unlock();
        }
    }
}
