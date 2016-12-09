package com.google.cloud.cache.apps.loadtest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;

/**
 * Track latency distrituion
 */
public final class LatencyTracker {

  private static final LatencyTracker me = new LatencyTracker();

  private LatencyStats myOpStats = new LatencyStats();

  private LatencyTracker() {}

  public static LatencyTracker getInstance() {
    return me;
  }

  public static LatencyTracker newInstance() {
    return new LatencyTracker();
  }

  void recordLatency(long nanoTime) {
    myOpStats.recordLatency(nanoTime);
  }

  String report() throws IOException {
    // Later, report on stats collected:
    Histogram intervalHistogram = myOpStats.getIntervalHistogram();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(os);
    intervalHistogram.outputPercentileDistribution(ps, 1000.0);
    return new String(os.toByteArray(), "UTF-8");
  }

  synchronized void reset() {
    myOpStats = new LatencyStats();
  }
}
