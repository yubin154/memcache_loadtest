package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// [START example]
/** run times of data store write + read ops */
public class DatastoreLoadTestServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(DatastoreLoadTestServlet.class.getName());
  private final DatastoreService datastore;

  public DatastoreLoadTestServlet() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    try {
      final int durationSec = reader.readDurationSec();
      final int frontendQps = reader.readFrontendQps();
      final int clientSize = reader.readClientSize();

      writer.write(
          String.format(
              "Setup load test num_of_client=%s, duration=%s, fe_qps=%s\n",
              clientSize, durationSec, frontendQps));
      final ExecutionTracker qpsTracker = ExecutionTracker.newInstance();
      final LatencyTracker latencyTracker = LatencyTracker.newInstance();
      List<DatastoreLoadTest> testers = new ArrayList<>();
      for (int i = 0; i < clientSize; i++) {
        final DatastoreLoadTest loadTester =
            new DatastoreLoadTest(qpsTracker, latencyTracker);
        testers.add(loadTester);
        new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    try {
                      loadTester.startTest(frontendQps);
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
        qpsTracker.getAndResetQps();
        qpsTracker.getAndResetErrorCount();
      }
      for (DatastoreLoadTest tester : testers) {
        tester.stopTest();
      }
      Thread.sleep(1000);
      writer.write(String.format("Average QPS %s\n", qpsTracker.averageQps()));
      writer.write(String.format("Error QPS %s\n", qpsTracker.averageErrors()));
      writer.write(latencyTracker.report());
      writer.write("\n");
    } catch (Exception e) {
      throw new IOException(e);
    }
    writer.write("done\n");
    writer.flush();
  }
}
// [END example]
