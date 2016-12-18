package com.epickrram.sync;

import java.util.concurrent.atomic.AtomicLong;

final class AtomicExchanger extends AbstractExchanger
{
    private final AtomicLong value = new AtomicLong();

    @Override
    public void updateCounter(final long v)
    {
        value.set(v);
    }

    @Override
    public long getCounter()
    {
        return value.get();
    }
}
