package com.epickrram.sync;

public class VolatileExchanger extends AbstractExchanger
{
    private volatile long value;

    @Override
    public void updateCounter(final long v)
    {
        value = v;
    }

    @Override
    public long getCounter()
    {
        return value;
    }
}
