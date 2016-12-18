package com.epickrram.sync;

import java.util.concurrent.atomic.AtomicLong;

final class LazyExchanger extends AbstractExchanger
{
    private final AtomicLong value = new AtomicLong();

    @Override
    public void updateCounter(final long v)
    {
        value.lazySet(v);
    }

    @Override
    public long getCounter()
    {
        return value.get();
    }
}
