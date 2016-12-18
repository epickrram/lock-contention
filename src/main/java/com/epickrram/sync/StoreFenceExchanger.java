package com.epickrram.sync;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class StoreFenceExchanger extends AbstractExchanger
{
    // pinched from https://raw.githubusercontent.com/LMAX-Exchange/disruptor/master/src/main/java/com/lmax/disruptor/util/Util.java
    private static final Unsafe THE_UNSAFE;

    static
    {
        try
        {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>()
            {
                public Unsafe run() throws Exception
                {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            };

            THE_UNSAFE = AccessController.doPrivileged(action);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }

    private long value;

    @Override
    public void updateCounter(final long v)
    {
        value = v;
        THE_UNSAFE.storeFence();
    }

    @Override
    public long getCounter()
    {
        return value;
    }
}
