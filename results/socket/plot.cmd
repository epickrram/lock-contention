set logscale x
unset xtics
set xlabel "Percentile"
set ylabel "Round-trip latency micros"
set title "Socket contention"
plot "shared-socket-histogram-1.data" using 4:1 with lines title "shared-socket-1", \
"shared-socket-histogram-2.data" using 4:1 with lines title "shared-socket-2", \
"duplex-socket-histogram-1.data" using 4:1 with lines title "duplex-socket-1", \
"duplex-socket-histogram-2.data" using 4:1 with lines title "duplex-socket-2", \
"duplex-socket-histogram-3.data" using 4:1 with lines title "duplex-socket-3", \
"xlabels.dat" with labels center offset 0, 1.5 point title ""

pause -1



#"shared-socket-histogram-3.data" using 4:1 with lines title "shared-socket-3", \
