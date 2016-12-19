package com.google.cloud.cache.apps.loadtest;

import static com.google.appengine.api.memcache.transcoders.AppEngineSerialization.makeKey;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

/** */
public final class TranscoderTest extends SpyMemcachedBaseTest {

  private MemcacheService aeClient = MemcacheServiceFactory.getMemcacheService();

  TranscoderTest(String server, int port, String version) {
    super(server, port, version, false);
  }

  public void testAll() throws Exception {
    testBytes();
    testStr();
  }

  public void testBytes() throws Exception {
    checkValuesInBothClients("zero byte", new byte[0]);
    byte[] data = new byte[] {1, 2, 3, 4, 5, 6};
    checkValuesInBothClients("bytes", data);
    data = new byte[] {65, 66, 67, 0, 68, 69, 70};
    checkValuesInBothClients("someBytesWithNull", data);
    //checkValuesInBothClients("bigKeyBytes", Strings.repeat("x", 300).getBytes());
  }

  public void testStr() throws Exception {
    checkValuesInBothClients("string", "some string");
    checkValuesInBothClients("empty string", "");
    checkValuesInBothClients("max string", "long string at max length(TBD)");
  }

  private void checkValuesInBothClients(String testDesc, Object obj) throws Exception {
    result.append(
        String.format("%s, class=%s, value=%s\n", testDesc, obj.getClass(), obj.toString()));
    String key1 = randomKey();
    // add by memcached, retriev from memcacheg
    expectTrue(client.add(makeKey(key1), DEFAULT_EXP, obj).get(), "add d2g");
    expectTrue(aeClient.get(key1).getClass().equals(obj.getClass()), "class verified");
    expectTrue(aeClient.get(key1).equals(obj), "value verified");

    String key2 = randomKey();
    // add by memcacheg, retriev from memcached
    expectTrue(
        aeClient.put(key2, obj, Expiration.byDeltaSeconds(10), SetPolicy.SET_ALWAYS),
        "add g2d");
    expectTrue(client.get(makeKey(key2)).getClass().equals(obj.getClass()), "class verified");
    expectTrue(client.get(makeKey(key2)).equals(obj), "value verified");
  }
}
