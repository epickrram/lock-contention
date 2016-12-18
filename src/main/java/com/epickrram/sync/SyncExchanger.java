package com.epickrram.sync;

final class SyncExchanger extends AbstractExchanger
{
    private final Object lock = new Object();
    private long value;

    @Override
    public void updateCounter(final long v)
    {
        synchronized (lock)
        {
            value = v;
        }
    }

    @Override
    public long getCounter()
    {
        synchronized (lock)
        {
            return value;
        }
    }
}
