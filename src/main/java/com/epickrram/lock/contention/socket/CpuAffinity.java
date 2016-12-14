package com.epickrram.lock.contention.socket;

import net.openhft.affinity.Affinity;

final class CpuAffinity
{
    private static final int UNSET_AFFINITY = -1;

    static int unSetAffinity()
    {
        return UNSET_AFFINITY;
    }

    static boolean isSet(final int cpuAffinity)
    {
        return cpuAffinity != UNSET_AFFINITY;
    }

    static void setAffinity(final int cpuAffinity)
    {
        if(isSet(cpuAffinity))
        {
            Affinity.setAffinity(cpuAffinity);
        }
    }
}
