package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
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

  void startAsyncTest(final Range<Integer> valueSizeRange, final int numOfThreads)
      throws Exception {
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      client.put(key, value);
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
  }

  void startSyncTest(final Range<Integer> valueSizeRange, final int numOfThreads) throws Exception {
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      syncClient.put(key, value);
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
                          Object obj = syncClient.get(key);
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
  }
}
