package com.epickrram.lock.contention;

import org.HdrHistogram.Histogram;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

final class HistogramReporter
{
    private static final int HIGHEST_TRACKABLE_VALUE = 50_000;
    private final Histogram histogram = new Histogram(HIGHEST_TRACKABLE_VALUE, 4);

    void recordValue(final long value)
    {
        histogram.recordValue(Math.min(HIGHEST_TRACKABLE_VALUE, TimeUnit.NANOSECONDS.toMicros(value)));
    }

    String report(final String histogramTitle) throws IOException
    {
        final Writer writer = new StringWriter();
        writer.append(format("== %s ==%n", histogramTitle));
        writer.append(format("%-8s%20d%n", "mean", (long) histogram.getMean()));
        writer.append(format("%-8s%20d%n", "min", histogram.getMinValue()));
        writer.append(format("%-8s%20d%n", "50.00%", histogram.getValueAtPercentile(50.0d)));
        writer.append(format("%-8s%20d%n", "90.00%", histogram.getValueAtPercentile(90.0d)));
        writer.append(format("%-8s%20d%n", "99.00%", histogram.getValueAtPercentile(99.0d)));
        writer.append(format("%-8s%20d%n", "99.90%", histogram.getValueAtPercentile(99.9d)));
        writer.append(format("%-8s%20d%n", "99.99%", histogram.getValueAtPercentile(99.99d)));
        writer.append(format("%-8s%20d%n", "99.999%", histogram.getValueAtPercentile(99.999d)));
        writer.append(format("%-8s%20d%n", "99.9999%", histogram.getValueAtPercentile(99.9999d)));
        writer.append(format("%-8s%20d%n", "max", histogram.getMaxValue()));
        writer.append(format("%-8s%20d%n", "count", histogram.getTotalCount()));
        writer.append("\n");
        writer.flush();

        return writer.toString();
    }
}
