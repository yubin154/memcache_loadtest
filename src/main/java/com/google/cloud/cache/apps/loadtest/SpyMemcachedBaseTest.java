package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.transcoders.AppEngineSerializingTranscoder;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;

abstract class SpyMemcachedBaseTest {

  private static final String LEASE_KEY = "LEASE";
  private static final int LEASE_TIMEOUT_SEC = 200;
  protected static final java.util.Random random = new java.util.Random();
  protected static final int DEFAULT_EXP = 0;

  private boolean testPassed = true;

  protected InetSocketAddress serverAddress;
  protected String version;
  protected boolean isAscii;
  protected StringBuffer result = new StringBuffer();

  protected MemcachedClient client;

  public SpyMemcachedBaseTest(String server, int port, String version, boolean isAscii) {
    this.serverAddress = new InetSocketAddress(server, port);
    this.version = version;
    this.isAscii = isAscii;
  }

  void setUp() throws Exception {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
    cfb.setTranscoder(new AppEngineSerializingTranscoder());
    cfb.setProtocol(isAscii ? Protocol.TEXT : Protocol.BINARY);
    ArrayList<InetSocketAddress> servers = new ArrayList<>();
    servers.add(serverAddress);
    client = new MemcachedClient(cfb.build(), servers);
  }

  void tearDown() throws Exception {
    client.shutdown();
  }

  protected void expectTrue(boolean condition, String template, Object... values) {
    result.append(isAscii ? "ascii " : "binary ");
    result.append(String.format(template, values));
    if (!condition) {
      testPassed = false;
      result.append(" [FAIL]").append("\n");
    } else {
      result.append(" [pass]").append("\n");
    }
  }

  protected void expectFalse(boolean condition, String template, Object... values) {
    expectTrue(!condition, template, values);
  }

  protected void expectEqual(Object expected, Object actual, String template, Object... values) {
    boolean condition =
        ((expected == null || expected instanceof byte[])
            ? (expected == actual)
            : expected.equals(actual));
    if (!condition) {
      result.append(String.format(" actual=%s ", actual));
    }
    expectTrue(condition, template, values);
  }

  public boolean isTestPassed() {
    return testPassed;
  }

  protected String setRandom(Object value) throws Exception {
    String key = randomKey();
    client.set(key, DEFAULT_EXP, value);
    return key;
  }

  protected String setByKey(String key) throws Exception {
    String value = randomValue();
    client.set(key, DEFAULT_EXP, value);
    return value;
  }

  protected static Collection<String> bulkKeys(int size) {
    List<String> keys = new ArrayList<>();
    int i = 0;
    while (i++ < size) {
      keys.add(randomKey());
    }
    return keys;
  }

  public static String randomKey() {
    return UUID.randomUUID().toString();
  }

  protected static byte[] randomBytes() {
    byte[] result = new byte[random.nextInt(4096)];
    random.nextBytes(result);
    return result;
  }

  protected static String randomValue() {
    return UUID.randomUUID().toString();
  }

  protected static int randomInteger() {
    return random.nextInt(Integer.MAX_VALUE);
  }

  protected boolean lease() {
    String leaseValue = randomValue();
    try {
      int retry = 0;
      do {
        if (client.add(LEASE_KEY, LEASE_TIMEOUT_SEC, leaseValue).get()) {
          Thread.sleep(1000);
          if (leaseValue.equals(client.get(LEASE_KEY))) {
            return true;
          } else {
            continue;
          }
        }
        Thread.sleep(1000);
      } while (retry++ < LEASE_TIMEOUT_SEC);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return false;
  }

  protected void release() {
    try {
      client.flush().get();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public String getResult() {
    return result.toString();
  }
}
