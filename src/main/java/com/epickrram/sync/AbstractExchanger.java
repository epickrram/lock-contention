package com.epickrram.sync;

public abstract class AbstractExchanger implements Exchanger
{
    private final UnprotectedState state = new UnprotectedState();

    @Override
    public long unprotectedGetValueForCounter(final long v)
    {
        return state.getValue(v);
    }

    @Override
    public void unprotectedSetValueForCounter(final long v)
    {
        state.setValue(v);
    }
}
