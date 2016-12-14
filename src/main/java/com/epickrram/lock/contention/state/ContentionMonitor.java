package com.epickrram.lock.contention.state;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

public final class ContentionMonitor
{
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    ContentionMonitor()
    {
        threadMXBean.setThreadContentionMonitoringEnabled(true);
    }

    public void monitor()
    {
        final ThreadInfo[] allThreads = threadMXBean.dumpAllThreads(false, false);
        Arrays.stream(allThreads).
                filter(t -> t.getThreadName().startsWith("client-handler")).
                forEach(t -> {
                    System.out.printf("%s blocked count: %d, time: %dms, waited count: %d, time: %dms%n",
                            t.getThreadName(), t.getBlockedCount(), t.getBlockedTime(),
                            t.getWaitedCount(), t.getWaitedTime());
        });
    }
}
