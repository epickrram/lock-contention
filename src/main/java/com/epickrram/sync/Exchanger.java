package com.epickrram.sync;

interface Exchanger
{
    void updateCounter(final long v);
    long getCounter();
    long unsafeGetValueForCounter(final long v);
    void unsafeSetValueForCounter(final long v);
}
