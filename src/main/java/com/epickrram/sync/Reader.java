package com.epickrram.sync;

final class Reader
{
    private final Exchanger exchanger;
    private long value;
    private long distinctUpdateCount = 0L;
    private long noUpdateCount = 0L;

    Reader(final Exchanger exchanger)
    {
        this.exchanger = exchanger;
    }

    public void run()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            final long current = exchanger.getCounter();

            final long state = exchanger.unprotectedGetValueForCounter(current);
            /*
            If this condition does not hold true, then the write before the
            counter-publish in writer thread has been re-ordered
             */
            if(state < current)
            {
                System.err.println(
                        String.format("Previous write for counter %d was not visible to reader!%n" +
                                "Expected %d > %d%n", current, state, current));
                return;
            }

            if(current != value)
            {
                distinctUpdateCount++;
            }
            else
            {
                noUpdateCount++;
            }
            value = current;
        }
    }

    long getValue()
    {
        return value;
    }

    long getDistinctUpdateCount()
    {
        return distinctUpdateCount;
    }

    long getNoUpdateCount()
    {
        return noUpdateCount;
    }
}
