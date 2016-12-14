package com.epickrram.lock.contention.state;

import java.util.Random;

public class WorkSimulator
{
    public void simulateWork()
    {
        final int sleepNanos = new Random(System.nanoTime()).nextInt(100);
        final long exitNanos = System.nanoTime() + sleepNanos;
        while(System.nanoTime() < exitNanos)
        {
            Thread.yield();
        }
    }
}
