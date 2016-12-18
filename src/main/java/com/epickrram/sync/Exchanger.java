package com.epickrram.sync;

interface Exchanger
{
    void updateCounter(final long v);
    long getCounter();
    long unprotectedGetValueForCounter(final long v);
    void unprotectedSetValueForCounter(final long v);
}
