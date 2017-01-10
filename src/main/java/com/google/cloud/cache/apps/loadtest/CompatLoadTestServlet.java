package com.google.cloud.cache.apps.loadtest;

import com.google.common.collect.Range;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Run 'times' of memcache Get requests + 1 Set request. */
public final class CompatLoadTestServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(CompatLoadTestServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    try {
      final Range<Integer> valueSizeRange = reader.readValueSizeRange();
      final int iterationCount = reader.readIterationCount();
      final int durationSec = reader.readDurationSec();
      final int frontendQps = reader.readFrontendQps();
      int clientSize = reader.readClientSize();

      writer.write(
          String.format(
              "Setup load test num_of_client=%s, duration=%s, fe_qps=%s\n",
              clientSize, durationSec, frontendQps));
      final ExecutionTracker qpsTracker = ExecutionTracker.newInstance();
      final LatencyTracker latencyTracker = LatencyTracker.newInstance();
      List<CompatLoadTest> testers = new ArrayList<>();
      for (int i = 0; i < clientSize; i++) {
        final CompatLoadTest loadTester =
            new CompatLoadTest(qpsTracker, latencyTracker);
        testers.add(loadTester);
        new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    try {
                      loadTester.startAsyncTest(valueSizeRange, frontendQps);
                    } catch (Exception e) {
                      logger.severe(e.getMessage());
                    }
                  }
                })
            .start();
      }
      qpsTracker.getAndResetQps();
      for (int i = 0; i <= durationSec; i++) {
        Thread.sleep(1000);
        writer.write(String.format("QPS %s\n", qpsTracker.getAndResetQps()));
        writer.write(String.format("Errors %s\n", qpsTracker.getAndResetErrorCount()));
      }
      for (CompatLoadTest tester : testers) {
        tester.stopTest();
      }
      Thread.sleep(1000);
      writer.write(latencyTracker.report());
      writer.write("\n");
    } catch (Exception e) {
      throw new IOException(e);
    }
    writer.write("done\n");
    writer.flush();
  }
}