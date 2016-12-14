package com.epickrram.lock.contention.state;

import java.util.LinkedList;

public final class SyncProtectedMutableState implements MutableState
{
    private static final int MAX_HISTORY = 20;
    private final WorkSimulator workSimulator = new WorkSimulator();
    private long value;
    private LinkedList<Long> history = new LinkedList<>();

    public synchronized long mutate(final Command command)
    {
        workSimulator.simulateWork();
        history.add(Long.valueOf(value));
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

    public synchronized long getValue()
    {
        workSimulator.simulateWork();
        return value;
    }

    public synchronized long getHistoric(final int age)
    {
        workSimulator.simulateWork();
        if(age >= MAX_HISTORY || age < 0)
        {
            return -1L;
        }

        return history.get(age);
    }
}
