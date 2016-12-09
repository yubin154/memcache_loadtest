package com.google.cloud.cache.apps.loadtest;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Run 'times' of memcache Get requests + 1 Set request. */
public final class TranscoderTestServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(TranscoderTestServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    final ResponseWriter writer = ResponseWriter.create(response);
    try {
      TranscoderTest tester =
          new TranscoderTest("169.254.10.1", 11211, "1.4.22");
      writer.write("Setup transcoder test\n");
      tester.setUp();
      tester.testStr();
      writer.write(tester.getResult());
      tester.tearDown();
    } catch (Exception e) {
      throw new IOException(e);
    }
    writer.write("done\n");
    writer.flush();
  }
}
