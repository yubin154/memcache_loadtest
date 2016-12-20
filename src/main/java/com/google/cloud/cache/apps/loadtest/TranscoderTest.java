package com.google.cloud.cache.apps.loadtest;

import static com.google.appengine.api.memcache.transcoders.Serialization.makeKey;
import static com.google.appengine.api.memcache.transcoders.Serialization.makeKeys;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;

/** */
public final class TranscoderTest extends SpyMemcachedBaseTest {

  private MemcacheService aeClient = MemcacheServiceFactory.getMemcacheService();

  TranscoderTest(String server, int port, String version) {
    super(server, port, version, false);
  }

  public void testAll() throws Exception {
    testStr();
    testBoolean();
    testInteger();
    testLong();
    testShort();
    testByte();
    testSerializableObject();
    testBytes();
    testIncrDecr();
    testDelete();
    testMultiKeySetGet();
    testCas();
    testNull();
  }

  public void testStr() throws Exception {
    checkValuesInBothClients("string", "some string");
    checkValuesInBothClients("empty string", "");
    checkValuesInBothClients("max string", "long string at max length(TBD)");
  }

  public void testBoolean() throws Exception {
    checkValuesInBothClients("false", Boolean.FALSE);
    checkValuesInBothClients("true", Boolean.TRUE);
  }

  public void testInteger() throws Exception {
    checkValuesInBothClients("integer zero", new Integer(0));
    checkValuesInBothClients("integer max", Integer.MAX_VALUE);
    checkValuesInBothClients("integer min", Integer.MIN_VALUE);
    checkValuesInBothClients("integer max overflow", Integer.MAX_VALUE + 1);
    checkValuesInBothClients("integer min overflow", Integer.MIN_VALUE - 1);
  }

  public void testLong() throws Exception {
    checkValuesInBothClients("long zero", new Long(0));
    checkValuesInBothClients("long max", Long.MAX_VALUE);
    checkValuesInBothClients("long min", Long.MIN_VALUE);
    checkValuesInBothClients("long max overflow", Long.MAX_VALUE + 1);
    checkValuesInBothClients("long min overflow", Long.MIN_VALUE - 1);
  }

  public void testShort() throws Exception {
    checkValuesInBothClients("short zero", new Short("0"));
    checkValuesInBothClients("short max", Short.MAX_VALUE);
    checkValuesInBothClients("short min", Short.MIN_VALUE);
    checkValuesInBothClients("short max overflow", Short.MAX_VALUE + 1);
    checkValuesInBothClients("short min overflow", Short.MIN_VALUE - 1);
  }

  public void testByte() throws Exception {
    checkValuesInBothClients("byte zero", (byte) 0);
    checkValuesInBothClients("byte max", Byte.MAX_VALUE);
    checkValuesInBothClients("byte min", Byte.MIN_VALUE);
    checkValuesInBothClients("byte max overflow", Byte.MAX_VALUE + 1);
    checkValuesInBothClients("byte min overflow", Byte.MIN_VALUE - 1);
  }

  public void testSerializableObject() throws Exception {
    java.util.Date dateObj = new java.util.Date();
    checkValuesInBothClients("date", dateObj);
    java.util.ArrayList listObj = new java.util.ArrayList();
    checkValuesInBothClients("list", listObj);
    java.util.HashMap mapObj = new java.util.HashMap();
    checkValuesInBothClients("map", mapObj);
  }

  public void testNull() throws Exception {
    checkValuesInBothClients("null", null);
  }

  public void testBytes() throws Exception {
    checkValuesInBothClients("zero byte", new byte[0]);
    byte[] data = new byte[] {1, 2, 3, 4, 5, 6};
    checkValuesInBothClients("bytes", data);
    data = new byte[] {65, 66, 67, 0, 68, 69, 70};
    checkValuesInBothClients("someBytesWithNull", data);
    //checkValuesInBothClients("bigKeyBytes", Strings.repeat("x", 300).getBytes());
  }

  public void testIncrDecr() throws Exception {
    result.append("\nTesting incr decr\n");
    String key1 = randomKey();
    result.append(String.format("d2g key=%s\n", key1));
    expectTrue(
        client.incr(makeKey(key1), 1, 10, DEFAULT_EXP) == 10, "increment with default value");
    expectTrue(client.incr(makeKey(key1), 1) == 11, "increment d");
    expectTrue(client.decr(makeKey(key1), 1) == 10, "decrement d");
    expectTrue(aeClient.increment(key1, 1) == 11, "increment g");
    expectTrue(aeClient.increment(key1, -1) == 10, "decrement g");
  }

  public void testDelete() throws Exception {
    result.append("\nTesting delete\n");
    String key1 = randomKey();
    result.append(String.format("d2g key=%s\n", key1));
    // add by memcached, delete from memcacheg
    expectTrue(client.add(makeKey(key1), DEFAULT_EXP, "").get(), "PUT");
    expectTrue(aeClient.delete(key1), "DELETE verified");

    String key2 = randomKey();
    result.append(String.format("g2d key=%s\n", key2));
    // add by memcacheg, delete from memcached
    aeClient.put(key2, "");
    expectTrue(client.delete(makeKey(key2)).get(), "DELETE verified");
  }

  public void testMultiKeySetGet() throws Exception {
    result.append("\nTesting multi-ket SETGET\n");
    Map<String, Object> map = new HashMap<>();
    List<String> keys = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String key = randomKey();
      keys.add(key);
      map.put(key, Integer.toString(i));
    }
    // add by memcacheg, retrieved from memcached
    aeClient.putAll(map);
    Map<String, Object> values = client.getBulk(makeKeys(keys));
    expectTrue(aeClient.getAll(keys).size() == 10, "GETALL verified");
    expectTrue(values.size() == 10, "GETBULK verified");
  }

  public void testCas() throws Exception {
    result.append("\nTesting cas\n");
    String key1 = randomKey();
    aeClient.put(key1, "value");
    // cas from g
    IdentifiableValue casValue = aeClient.getIdentifiable(key1);
    client.add(makeKey(key1), DEFAULT_EXP, "value").get();
    expectFalse(aeClient.putIfUntouched(key1, casValue, "valueg"), "CAS4g verified");
    // cas from d
    CASValue<Object> cas = client.gets(makeKey(key1));
    aeClient.put(key1, "value");
    expectTrue(
        client.cas(makeKey(key1), cas.getCas(), "valued").equals(CASResponse.EXISTS),
        "CAS4d verified");
  }

  private void checkValuesInBothClients(String testDesc, Object obj) throws Exception {
    if (obj != null) {
      result.append(
          String.format("\nTesting %s, %s=%s\n", testDesc, obj.getClass().getName(), obj));
    } else {
      result.append(String.format("\nTesting %s, value=null\n", testDesc));
    }
    String key1 = randomKey();
    result.append(String.format("d2g key=%s\n", key1));
    // add by memcached, retrieved from memcacheg
    expectTrue(client.add(makeKey(key1), DEFAULT_EXP, obj).get(), "PUT");
    expectEqual(obj, aeClient.get(key1), "GET verified");

    String key2 = randomKey();
    result.append(String.format("g2d key=%s\n", key2));
    // add by memcacheg, retrieved from memcached
    aeClient.put(key2, obj);
    expectEqual(obj, aeClient.get(key2), "PUT");
    expectEqual(obj, client.get(makeKey(key2)), "GET verified");
  }
}
