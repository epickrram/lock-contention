package com.epickrram.sync;

final class Writer
{
    private final Exchanger exchanger;
    private long value;

    Writer(final Exchanger exchanger)
    {
        this.exchanger = exchanger;
    }

    public void run()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            final long updated = value++;
            exchanger.unprotectedSetValueForCounter(updated);
            exchanger.updateCounter(updated);
        }
    }
}
