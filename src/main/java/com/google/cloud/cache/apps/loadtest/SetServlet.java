package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheSerialization;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Range;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cache a random value for a key.
 *
 * <p>If no key is given, select a random one.You can choose a key space size for random keys with
 * the key_space_size parameter.
 *
 * <p>If 'times' is specified, repeat the operation that number of times.
 */
public final class SetServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(SetServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    Range<Integer> valueSizeRange = reader.readValueSizeRange();
    int iterationCount = reader.readIterationCount();
    for (int i = 0; i < iterationCount; ++i) {
      java.io.Serializable key = new java.util.Date();
      writer.write(String.format("key=%s, encodedKey=%s", key, getEncodedKey(key)));
      String value = MemcacheValues.random(valueSizeRange);
      try {
        memcache.put(key, value);
      } catch (Exception e) {
        logger.severe("Memcache set failed for key: " + key);
        writer.fail();
        return;
      }
      writer.write(key.toString(), value);
    }
  }

  static String getEncodedKey(java.io.Serializable key)
      throws UnsupportedEncodingException, IOException {
    return ByteString.copyFrom(MemcacheSerialization.makePbKey(key)).toString("UTF-8");
  }
}
