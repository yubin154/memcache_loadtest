package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.AsyncMemcacheService;
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

/** Run 'times' of memcache Get requests + 1 Set request. */
public final class QpsLoadTestServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(QpsLoadTestServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    final AsyncMemcacheService memcache = MemcacheServiceFactory.getAsyncMemcacheService();

    Range<Integer> valueSizeRange = reader.readValueSizeRange();
    final int iterationCount = reader.readIterationCount();
    final int durationSec = reader.readDurationSec();
    int frontendQps = reader.readFrontendQps();
    List<Future> futures = new ArrayList<>();
    final ExecutionTracker qpsTracker = ExecutionTracker.newInstance();
    for (int i = 0; i < frontendQps; ++i) {
      final String key = UUID.randomUUID().toString();
      final Object value = MemcacheValues.random(valueSizeRange);
      memcache.put(key, value);
      qpsTracker.incrementQps();
      futures.add(
          ExecutionTracker
              .getExecutorService()
              .submit(
                  new Runnable() {
                    @Override
                    public void run() {
                      int duration = durationSec;
                      List<Future> opsFutures = new ArrayList<>();
                      try {
                        do {
                          opsFutures.clear();
                          long start = System.currentTimeMillis();
                          for (int i = 0; i < iterationCount; ++i) {
                            opsFutures.add(memcache.get(key));
                            qpsTracker.incrementQps();
                          }
                          for (Future future : opsFutures) {
                            if (future.get() == null) {
                              qpsTracker.incrementErrorCount();
                            }
                          }
                          if (duration > 0) {
                            // sleep till 1 second
                            Thread.sleep(Math.max(1, 1000 - (System.currentTimeMillis() - start)));
                          }
                        } while (duration-- >= 0);
                      } catch (Throwable t) {
                        qpsTracker.incrementErrorCount();
                      }
                    }
                  }));
    }
    for (int i = 0; i <= durationSec; ++i) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      writer.write(String.format("QPS %s\n", qpsTracker.getAndResetQps()));
      writer.write(
          String.format("Errors %s\n", qpsTracker.getAndResetErrorCount()));
      writer.flush();
    }
    for (Future future : futures) {
      try {
        future.get();
      } catch (Exception e) {
        writer.write(e.getMessage());
      }
    }
    writer.write("done");
  }
}
