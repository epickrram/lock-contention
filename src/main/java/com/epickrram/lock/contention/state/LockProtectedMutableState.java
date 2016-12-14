package com.epickrram.lock.contention.state;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LockProtectedMutableState implements MutableState
{
    private static final int MAX_HISTORY = 20;
    private final Lock lock = new ReentrantLock();
    private final WorkSimulator workSimulator = new WorkSimulator();
    private long value;
    private LinkedList<Long> history = new LinkedList<>();

    @Override
    public long mutate(final Command command)
    {
        lock.lock();
        try
        {
            workSimulator.simulateWork();
            history.addFirst(Long.valueOf(value));
            if(history.size() > MAX_HISTORY)
            {
                history.removeLast();
            }
            switch(command.getOperation())
            {
                case ADD:
                    value += command.getOperand();
                    break;
                case SUBTRACT:
                    value -= command.getOperand();
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            return value;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public long getValue()
    {
        lock.lock();
        try
        {
            workSimulator.simulateWork();
            return value;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public long getHistoric(final int age)
    {
        lock.lock();
        try
        {
            workSimulator.simulateWork();
            if (age >= MAX_HISTORY || age < 0)
            {
                return -1L;
            }

            return history.get(age);
        }
        finally
        {
            lock.unlock();
        }
    }
}
