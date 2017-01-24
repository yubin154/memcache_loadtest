package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

final class DatastoreLoadTest extends BaseTest {

  private ExecutionTracker qpsTracker;
  private LatencyTracker latencyTracker;
  private DatastoreService client;

  DatastoreLoadTest(ExecutionTracker qpsTracker, LatencyTracker latencyTracker) {
    this.qpsTracker = qpsTracker;
    this.latencyTracker = latencyTracker;
    this.client = DatastoreServiceFactory.getDatastoreService();
  }

  void startTest(final int numOfThreads)
      throws Exception {
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < numOfThreads; ++i) {
      String key = randomKey();
      DatastoreStressOperations.write(client, key, false);
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker.getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      try {
                        while (!testStopped()) {
                          long start = System.nanoTime();
                          Entity entity = DatastoreStressOperations.read(client, key);
                          latencyTracker.recordLatency(System.nanoTime() - start);
                          if (entity != null && entity.getKey().equals(key)) {
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
