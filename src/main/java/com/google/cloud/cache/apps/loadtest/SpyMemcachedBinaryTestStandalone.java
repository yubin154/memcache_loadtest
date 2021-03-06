package com.google.cloud.cache.apps.loadtest;

import java.util.Collection;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;

final class SpyMemcachedBinaryTestStandalone extends SpyMemcachedBaseTest {

  SpyMemcachedBinaryTestStandalone(String server, int port, String version) {
    super(server, port, version, false);
  }

  void runAllTests() throws Exception {
    if (!lease()) {
      return;
    }
    try {
      testAdd();
      testAppend();
      testBulkGet();
      testCas();
      testGet();
      testGets();
      testGetVersion();
      testIncr();
      testDecr();
      testDelete();
      testFlush();
      if (!lease()) {
        return;
      }
      testPrepend();
      testReplace();
      testSet();
      testStats();
    } finally {
      release();
    }
  }

  private void testAdd() throws Exception {
    // add
    String key = randomKey();
    expectTrue(client.add(key, DEFAULT_EXP, randomValue()).get(), "add");
    // add again
    expectFalse(client.add(key, DEFAULT_EXP, randomValue()).get(), "add noreply");
  }

  private void testAppend() throws Exception {
    // append
    String key = randomKey();
    String value = setByKey(key);
    String valueToAppend = randomValue();
    CASValue<Object> cas = client.gets(key);
    // append nonexist
    expectFalse(
        client.append(cas.getCas(), "notexit", valueToAppend).get(), "append noreply");
    // casid honored in binary protocol
    // expectFalse(client.append(1L, key, valueToAppend).get(), "testAppend invalid casid");
    cas = client.gets(key);
    expectTrue(client.append(cas.getCas(), key, valueToAppend).get(), "append");
    expectTrue(client.get(key).equals(value + valueToAppend), "append");
  }

  public void testBulkGet() throws Exception {
    Collection<String> keys = bulkKeys(10);
    expectTrue(client.getBulk(keys).isEmpty(), "getbulk noreply");
    String value = setByKey(keys.iterator().next());
    expectTrue(client.getBulk(keys).size() == 1, "getbulk");
  }

  public void testCas() throws Exception {
    String key = setRandom(randomValue());
    CASValue<Object> cas = client.gets(key);
    expectTrue(client.cas(key, cas.getCas(), randomValue()).equals(CASResponse.OK), "cas");
    // set again
    expectTrue(
        client.cas(key, cas.getCas(), randomValue()).equals(CASResponse.EXISTS),
        "cas noreply");
    client.delete(key);
    // set again
    expectTrue(
        client.cas(key, cas.getCas(), randomValue()).equals(CASResponse.NOT_FOUND),
        "cas noreply");
  }

  public void testGet() throws Exception {
    expectTrue(client.get(randomKey()) == null, "get noreply");
    String key = setRandom("value1");
    expectTrue(client.get(key).equals("value1"), "get");
  }

  public void testGets() throws Exception {
    expectTrue(client.gets(randomKey()) == null, "gets noreply");
    String value = randomValue();
    String key = setRandom(value);
    expectTrue(client.gets(key).getValue().equals(value), "gets");
  }

  public void testGetVersion() throws Exception {
    // verion
    expectTrue(client.getVersions().get(serverAddress).contains(version), "version");
  }

  public void testIncr() throws Exception {
    String key = randomKey();
    // incr nonexist
    //expectTrue(client.incr(key, 1) == -1, "testIncr incr nonexist");
    // incr with default value
    expectTrue(client.incr(key, 1, 10, DEFAULT_EXP) == 10, "incr");
    // incr
    expectTrue(client.incr(key, 1) == 11, "incr");
  }

  public void testDecr() throws Exception {
    String key = randomKey();
    // decr nonexist
    //expectTrue(client.decr(key, 1) == -1, "testDecr decr nonexist");
    // decr with default value
    expectTrue(client.decr(key, 1, 10, DEFAULT_EXP) == 10, "decr");
    // decr
    expectTrue(client.decr(key, 1) == 9, "decr");
  }

  public void testDelete() throws Exception {
    String key = setRandom(randomValue());
    expectTrue(client.delete(key).get(), "delete");
    // delete again
    expectFalse(client.delete(key).get(), "delete noreply");
  }

  public void testFlush() throws Exception {
    String key = setRandom(randomBytes());
    expectTrue(client.flush().get(), "flush");
    expectTrue(client.get(key) == null, "flush noreply");
  }

  public void testPrepend() throws Exception {
    // prepend
    String key = randomKey();
    String value = setByKey(key);
    String valueToPrepend = randomValue();
    CASValue<Object> cas = client.gets(key);
    // prepend nonexist
    expectFalse(
        client.prepend(cas.getCas(), "notexit", valueToPrepend).get(), "prepend noreply");
    // prepend, casid is honored in binary
    // expectFalse(client.prepend(1L, key, valueToPrepend).get(), "testPrepend invalid casid");
    cas = client.gets(key);
    expectTrue(client.prepend(cas.getCas(), key, valueToPrepend).get(), "prepend");
    expectTrue(client.get(key).equals(valueToPrepend + value), "prepend");
  }

  public void testReplace() throws Exception {
    String key = randomKey();
    // nonexist
    expectFalse(client.replace(key, DEFAULT_EXP, randomValue()).get(), "replace noreply");
    String value = setByKey(key);
    String valueToReplace = randomValue();
    expectTrue(client.replace(key, DEFAULT_EXP, valueToReplace).get(), "replace");
    expectTrue(client.get(key).equals(valueToReplace), "replace");
  }

  public void testSet() throws Exception {
    String key = randomKey();
    String value = randomValue();
    expectTrue(client.set(key, 0, value).get(), "set");
    expectTrue(client.get(key).equals(value), "set");
    // set expire in 1 second
    String anotherValue = randomValue();
    expectTrue(client.set(key, 1, anotherValue).get(), "set");
    expectTrue(client.get(key).equals(anotherValue), "set");
    Thread.sleep(1000);
    expectTrue(client.get(key) == null, "set noreply");
  }

  public void testStats() throws Exception {
    expectTrue(client.getStats().containsKey(serverAddress), "stats");
  }
}
