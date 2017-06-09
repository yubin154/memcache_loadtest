package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

final class CompatLoadTest extends BaseTest {

  private ExecutionTracker qpsTracker;
  private LatencyTracker latencyTracker;
  private AsyncMemcacheService client;
  private MemcacheService syncClient;

  CompatLoadTest(ExecutionTracker qpsTracker, LatencyTracker latencyTracker) {
    this.qpsTracker = qpsTracker;
    this.latencyTracker = latencyTracker;
    this.client = MemcacheServiceFactory.getAsyncMemcacheService();
    this.syncClient = MemcacheServiceFactory.getMemcacheService();
  }

  void startAsyncTest(
      final Range<Integer> valueSizeRange,
      final int numOfThreads,
      final int batchSize,
      final int retryAttempt)
      throws Exception {
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      final ImmutableList<String> keys = MemcacheValues.randomKeys(batchSize);
      final Object value = MemcacheValues.random(valueSizeRange);
      for (String key : keys) {
        client.put(key, value);
      }
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker.getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      try {
                        int currentRetryAttempt = 0;
                        while (!testStopped()) {
                          long start = System.nanoTime();
                          Map values = client.getAll(keys).get();
                          latencyTracker.recordLatency(System.nanoTime() - start);
                          if (!values.isEmpty()) {
                            qpsTracker.incrementQps();
                            if (currentRetryAttempt > 0) {
                              currentRetryAttempt = 0;
                            }
                          } else {
                            if (currentRetryAttempt++ > retryAttempt) {
                              qpsTracker.incrementErrorCount();
                              currentRetryAttempt = 0;
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
  }

  void startSyncTest(
      final Range<Integer> valueSizeRange, final int numOfThreads, final int retryAttempt)
      throws Exception {
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      syncClient.put(key, value);
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker.getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      try {
                        while (!testStopped()) {
                          int currentRetryAttempt = 0;
                          long start = System.nanoTime();
                          Object obj = syncClient.get(key);
                          latencyTracker.recordLatency(System.nanoTime() - start);
                          if (obj != null) {
                            qpsTracker.incrementQps();
                            if (currentRetryAttempt > 0) {
                              currentRetryAttempt = 0;
                            }
                          } else {
                            if (currentRetryAttempt++ > retryAttempt) {
                              qpsTracker.incrementErrorCount();
                              currentRetryAttempt = 0;
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
  }
}
