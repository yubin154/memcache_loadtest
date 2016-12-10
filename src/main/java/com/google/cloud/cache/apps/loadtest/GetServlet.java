package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.spy.memcached.MemcachedClient;

/**
 * Get value for a key, setting to a random string if unset.
 *
 * <p>If no key is given, select a random one. You can choose a key space size for random keys with
 * the key_space_size parameter.
 *
 * <p>If 'times' is specified, repeat the operation that number of times.
 */
public final class GetServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(GetServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);

    String key = reader.readKey();
    Object value = null;
    Object valueg = null;
    Object valued = null;
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    MemcachedClient client = SpymemcachedUtil.binaryClient();
    Range<Integer> valueSizeRange = reader.readValueSizeRange();

    if (key == null) {
      key = MemcacheValues.randomKey();
    }

    value = MemcacheValues.random(valueSizeRange);
    try {
      if (!reader.isMemcacheg()) {
        client.add(key, 0, value).get();
      } else {
        memcache.put(key, value);
      }
      valueg = memcache.get(key);
      valued = client.get(key);
    } catch (Exception e) {
      logger.severe("Memcache set failed for key: " + key);
      writer.fail();
      return;
    }
    writer.write(
        String.format(
            "key=%s\nvalue=%s\nvalueg=%s\nvalued=%s",
            key,
            (value == null ? "NULL" : value.toString()),
            (valueg == null ? "NULL" : valueg.toString()),
            (valued == null ? "NULL" : valued.toString())));
  }
}
