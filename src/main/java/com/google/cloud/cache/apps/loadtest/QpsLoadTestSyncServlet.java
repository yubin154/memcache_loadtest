package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Run 'times' of memcache Get requests + 1 Set request, use synchronous memcache service */
public final class QpsLoadTestSyncServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(QpsLoadTestSyncServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    Range<Integer> valueSizeRange = reader.readValueSizeRange();
    final int iterationCount = reader.readIterationCount();
    final int durationSec = reader.readDurationSec();
    int frontendQps = reader.readFrontendQps();
    List<Future> futures = new ArrayList<>();
    final LatencyTracker latencyTracker = LatencyTracker.newInstance();
    for (int i = 0; i < frontendQps; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      memcache.put(key, value);
      ExecutionTracker.getInstance().incrementQps();
      futures.add(
          ExecutionTracker.getInstance()
              .getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      long durationMs = durationSec * 1000L;
                      long startMs = System.currentTimeMillis();
                      try {
                        do {
                          long start = System.nanoTime();
                          Object obj = memcache.get(key);
                          latencyTracker.recordLatency(System.nanoTime() - start);
                          if (obj == null) {
                            ExecutionTracker.getInstance().incrementErrorCount();
                          } else {
                            ExecutionTracker.getInstance().incrementQps();
                          }
                        } while ((System.currentTimeMillis() - startMs) < durationMs);
                      } catch (Throwable t) {
                        ExecutionTracker.getInstance().incrementErrorCount();
                      }
                    }
                  }));
    }
    for (int i = 0; i <= durationSec; i++) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        writer.write(e.getMessage());
      }
      writer.write(String.format("QPS %s\n", ExecutionTracker.getInstance().getAndResetQps()));
      writer.write(
          String.format("Errors %s\n", ExecutionTracker.getInstance().getAndResetErrorCount()));
      writer.flush();
    }
    for (Future future : futures) {
      try {
        future.get();
      } catch (Exception e) {
        writer.write(e.getMessage());
      }
    }
    writer.write(latencyTracker.report());
    writer.write("\n");
    writer.write("done");
  }
}
