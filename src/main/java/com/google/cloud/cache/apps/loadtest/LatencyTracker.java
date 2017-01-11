package com.google.cloud.cache.apps.loadtest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.HdrHistogram.Histogram;

/** Track latency distrituion */
public final class LatencyTracker {

  // A Histogram covering the range from 1 nsec to 1 hour with 3 decimal point resolution:
  private Histogram histogram = new Histogram(3600000000000L, 3);

  private LatencyTracker() {}

  public static LatencyTracker newInstance() {
    return new LatencyTracker();
  }

  void recordLatency(long nanoTime) {
    histogram.recordValue(nanoTime);
  }

  String report() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(os);
    // Report micro-second, 2 ticks per half percentile.
    histogram.outputPercentileDistribution(ps, 2, 1000.0);
    return new String(os.toByteArray(), "UTF-8");
  }

  synchronized void reset() {
    histogram.reset();
  }
}
