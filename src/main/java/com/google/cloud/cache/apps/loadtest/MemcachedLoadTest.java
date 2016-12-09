package com.google.cloud.cache.apps.loadtest;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

final class MemcachedLoadTest extends SpyMemcachedBaseTest {

  private volatile boolean stop = false;
  private ExecutionTracker qpsTracker;
  private LatencyTracker latencyTracker;

  MemcachedLoadTest(
      String server,
      int port,
      String version,
      ExecutionTracker qpsTracker,
      LatencyTracker latencyTracker) {
    super(server, port, version, false);
    this.qpsTracker = qpsTracker;
    this.latencyTracker = latencyTracker;
  }

  void stopTest() {
    this.stop = true;
  }

  private boolean testStopped() {
    return stop;
  }

  void startTest(final Range<Integer> valueSizeRange, final int numOfThreads) throws Exception {
    this.setUp();
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      client.set(key, 0, value).get();
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker.getInstance()
              .getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      try {
                        while (!testStopped()) {
                          long start = System.nanoTime();
                          Object obj = client.get(key);
                          latencyTracker.recordLatency(System.nanoTime() - start);
                          if (obj != null) {
                            qpsTracker.incrementQps();
                          } else {
                            qpsTracker.incrementErrorCount();
                          }
                        }
                      } catch (Throwable t) {
                        qpsTracker.incrementErrorCount();
                      }
                    }
                  }));
    }
    for (Future future : futures) {
      future.get();
    }
    this.tearDown();
  }
}
