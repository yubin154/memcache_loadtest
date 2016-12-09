package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Increment then decrement value for a key.
 *
 * <p>If no key is given, select a random one. You can choose a key space size for random keys with
 * the key_space_size parameter.
 *
 * <p>If 'times' is specified, repeat the operation that number of times.
 */
public final class IncrementDecrementServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(IncrementDecrementServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    String key = reader.readKey();
    int iterationCount = reader.readIterationCount();
    for (int i = 0; i < iterationCount; ++i) {
      memcache.increment(key, 1, 0L);
      Long value = memcache.increment(key, -1);
      if (value == null) {
        logger.severe("Memcache incr/decr failed for key: " + key);
        writer.fail();
        return;
      }
      writer.write(key, value.toString());
    }
  }
}
