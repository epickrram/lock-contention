#!/bin/bash

java -XX:+PreserveFramePointer -cp build/libs/lock-contention.jar com.epickrram.sync.TestRunner $@ &
SLEEP_SEC=60
echo "Sleeping for $SLEEP_SEC to allow warm-up"

sleep $SLEEP_SEC

JVM_PID=$(ps aux | grep TestRunner | grep -v grep | awk '{print $2}')
HEX_PID=$(jstack $JVM_PID | grep contention-test-writer | awk -F 'nid=' '{print $2}' | awk '{print $1}' | awk -F'x' '{print $2}')
WRITER_PID=$((16#$HEX_PID))

bash ~/dev/perf-map-agent/bin/create-java-perf-map.sh $JVM_PID

sudo perf record -F 10000 -e instructions,cycles -g -t $WRITER_PID &

sleep $SLEEP_SEC

sudo pkill perf

pkill -f TestRunner
