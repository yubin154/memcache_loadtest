package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Set then Get then Delete for a key.
 *
 * <p>If no key is given, select a random one. You can choose a key space size for random keys with
 * the key_space_size parameter.
 *
 * <p>If 'times' is specified, repeat the operation that number of times
 */
public final class SetGetDeleteServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(SetGetDeleteServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    String key = reader.readKey();
    Range<Integer> valueSizeRange = reader.readValueSizeRange();
    int iterationCount = reader.readIterationCount();
    for (int i = 0; i < iterationCount; ++i) {
      String value = MemcacheValues.random(valueSizeRange);
      try {
        memcache.put(key, value);
      } catch (Exception e) {
        logger.severe("Memcache set failed for key: " + key);
        writer.fail();
        return;
      }

      if (memcache.get(key) == null) {
        logger.severe("Memcache get failed for key: " + key);
        writer.fail();
        return;
      }

      if (memcache.delete(key)) {
        logger.severe("Memcache delete failed for key: " + key);
        writer.fail();
        return;
      }

      writer.write(key, value);
    }
  }
}
