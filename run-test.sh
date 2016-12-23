#!/bin/bash

java -XX:+PreserveFramePointer -cp build/libs/lock-contention.jar com.epickrram.sync.TestRunner $@ &
SLEEP_SEC=60

sleep 1

JVM_PID=$(ps aux | grep TestRunner | grep -v grep | awk '{print $2}')
HEX_PID=$(jstack $JVM_PID | grep contention-test-writer | awk -F 'nid=' '{print $2}' | awk '{print $1}' | awk -F'x' '{print $2}')
WRITER_PID=$((16#$HEX_PID))

bash ~/dev/perf-map-agent/bin/create-java-perf-map.sh $JVM_PID

echo "trace-cmd record -e "sched:sched_stat*" -e "sched:sched_wa*" -e "sched:sched_switch" -P $WRITER_PID -T"

echo "Sleeping for $SLEEP_SEC to allow warm-up"

sleep $SLEEP_SEC

echo "Warmup period complete"

#sudo perf record -F 10000 -e instructions,cycles -g -t $WRITER_PID &
#sudo trace-cmd record -e "sched:sched_stat*" -e "sched:sched_wa*" -e "sched:sched_switch" -P $WRITER_PID -T

sleep $SLEEP_SEC

#sudo pkill perf
#sudo pkill trace-cmd

pkill -f TestRunner

