package com.google.cloud.cache.apps.loadtest;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

final class MemcachedLoadTest extends SpyMemcachedBaseTest {

  private ExecutionTracker qpsTracker;
  private LatencyTracker latencyTracker;

  MemcachedLoadTest(
      String server,
      int port,
      String version,
      boolean requireSasl,
      ExecutionTracker qpsTracker,
      LatencyTracker latencyTracker) {
    super(server, port, version, false, requireSasl);
    this.qpsTracker = qpsTracker;
    this.latencyTracker = latencyTracker;
  }

  void startTest(
      final Range<Integer> valueSizeRange, final int numOfThreads, final int retryAttempt)
      throws Exception {
    this.setUp();
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker.getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      int retryCounter = 0;
                      try {
                        String key = randomSet(valueSizeRange);
                        while (!testStopped()) {
                          try {
                            long start = System.nanoTime();
                            Object obj = client.get(key);
                            latencyTracker.recordLatency(System.nanoTime() - start);
                            if (obj != null) {
                              // Get hit
                              qpsTracker.incrementQps();
                              if (retryCounter > 0) {
                                retryCounter = 0;
                              }
                            } else {
                              // Get miss, retry up to retryAttempt
                              if (++retryCounter > retryAttempt) {
                                qpsTracker.incrementMissCount();
                                retryCounter = 0;
                                key = randomSet(valueSizeRange);
                              }
                            }
                          } catch (Exception e) {
                            // Checked exception including RPC errors, retry up to retryAttempt
                            if (++retryCounter > retryAttempt) {
                              qpsTracker.incrementErrorCount();
                              retryCounter = 0;
                              key = randomSet(valueSizeRange);
                            }
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

  String randomSet(Range<Integer> valueSizeRange) throws InterruptedException, ExecutionException {
    final String key = UUID.randomUUID().toString();
    final Object value = MemcacheValues.random(valueSizeRange);
    client.set(key, 0, value).get();
    return key;
  }
}
