package com.google.cloud.cache.apps.loadtest;

import static com.google.common.base.Functions.constant;
import static com.google.common.collect.Maps.toMap;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * MultiSet random values for keys.
 *
 * <p>If no key is given, select a random one. You can choose a key space size for random keys with
 * the key_space_size parameter.
 *
 * <p>If 'times' is specified, repeat the operation that number of times.
 */
public final class MultiSetServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(MultiSetServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    ImmutableList<String> keys = reader.readKeys();
    Range<Integer> valueSizeRange = reader.readValueSizeRange();
    int iterationCount = reader.readIterationCount();
    for (int i = 0; i < iterationCount; ++i) {
      String value = MemcacheValues.random(valueSizeRange);
      ImmutableMap<String, String> map = toMap(keys, constant(value));
      try {
        memcache.putAll(map);
      } catch (Exception e) {
        logger.severe("Memcache set_multi failed.");
        writer.fail();
        return;
      }
      writer.write(keys.toString(), value);
    }
  }
}
