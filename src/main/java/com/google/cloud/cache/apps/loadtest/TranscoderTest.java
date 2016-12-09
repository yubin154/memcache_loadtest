package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/** */
public final class TranscoderTest extends SpyMemcachedBaseTest {

  private MemcacheService aeClient = MemcacheServiceFactory.getMemcacheService();

  TranscoderTest(String server, int port, String version) {
    super(server, port, version, false);
  }

  public void testStr() throws Exception {
    String key1 = randomKey();
    String strVal1 = randomValue();
    expectTrue(client.add(key1, DEFAULT_EXP, strVal1).get(), "add d2g str");
    expectTrue(aeClient.get(key1).equals(strVal1), "verified");

    String key2 = randomKey();
    String strVal2 = randomValue();
    expectTrue(
        aeClient.put(
            key2, strVal2, Expiration.byDeltaSeconds(10), SetPolicy.ADD_ONLY_IF_NOT_PRESENT),
        "add g2d str");
    expectTrue(client.get(key2).equals(strVal2), "verified");
  }
}
