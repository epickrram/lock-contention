package com.epickrram.sync;

public abstract class AbstractExchanger implements Exchanger
{
    private final UnprotectedState state = new UnprotectedState();

    @Override
    public long unsafeGetValueForCounter(final long v)
    {
        return state.getValue(v);
    }

    @Override
    public void unsafeSetValueForCounter(final long v)
    {
        state.setValue(v);
    }
}
