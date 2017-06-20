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
public final class MemcachedLoadTestServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(MemcachedLoadTestServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    final ExecutionTracker qpsTracker = ExecutionTracker.newInstance();
    final LatencyTracker latencyTracker = LatencyTracker.newInstance();
    try {
      final Range<Integer> valueSizeRange = reader.readValueSizeRange();
      final int iterationCount = reader.readIterationCount();
      final int durationSec = reader.readDurationSec();
      final int frontendQps = reader.readFrontendQps();
      final int retryAttempt = reader.retryAttempt();
      final String memcachedHost = reader.readMemcachedHost();
      int clientSize = reader.readClientSize();

      writer.write(
          String.format(
              "Setup load test num_of_client=%s, duration=%s, fe_qps=%s, retryAttempt=%s\n",
              clientSize, durationSec, frontendQps, retryAttempt));
      List<MemcachedLoadTest> testers = new ArrayList<>();
      for (int i = 0; i < clientSize; i++) {
        final MemcachedLoadTest loadTester =
            new MemcachedLoadTest(memcachedHost, 11211, "1.4.22", qpsTracker, latencyTracker);
        testers.add(loadTester);
        new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    try {
                      loadTester.startTest(valueSizeRange, frontendQps, retryAttempt);
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
      }
      for (MemcachedLoadTest tester : testers) {
        tester.stopTest();
      }
      Thread.sleep(1000);
      writer.write(String.format("QPS %s\n", qpsTracker.averageQps()));
      writer.write(String.format("MissQPS %s\n", qpsTracker.averageMisses()));
      writer.write(
          String.format(
              "MissRate %.4f\n", (qpsTracker.averageMisses() / qpsTracker.averageQps())));
      writer.write(String.format("ErrorQPS %s\n", qpsTracker.averageErrors()));
      writer.write(
          String.format(
              "ErrorRate %.4f\n", (qpsTracker.averageErrors() / qpsTracker.averageQps())));
      writer.write(latencyTracker.report());
      writer.write("\n");
    } catch (Exception e) {
      throw new IOException(e);
    }
    double errorRate = qpsTracker.averageErrors() / qpsTracker.averageQps();
    // Test fail if throughput is low or error rate is high
    writer.write((qpsTracker.averageQps() < 100 || errorRate >= 0.01) ? "FAIL\n" : "PASS\n");
    writer.flush();
  }
}
