package com.epickrram.lock.contention.state;

public interface MutableState
{
    long mutate(Command command);

    long getValue();

    long getHistoric(int age);
}
