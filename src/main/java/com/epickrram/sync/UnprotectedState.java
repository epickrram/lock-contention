package com.epickrram.sync;

public final class UnprotectedState
{
    private static final int LENGTH = 64;
    private static final int MASK = LENGTH - 1;
    private long[] counterTracker = new long[LENGTH];

    void setValue(final long counterValue)
    {
        counterTracker[mask(counterValue)] = counterValue;
    }

    long getValue(final long counterValue)
    {
        return counterTracker[mask(counterValue)];
    }

    private static int mask(final long counterValue)
    {
        return ((int) counterValue) & MASK;
    }
}
