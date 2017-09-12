package com.google.cloud.cache.apps.loadtest;

// import com.google.appengine.api.memcache.transcoders.SpymemcachedSerializingTranscoder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;

abstract class SpyMemcachedBaseTest extends BaseTest {

  private static Object SASL_INITIALIZAQTION_LOCK = new Object();
  private static boolean SASL_INITIALIZED = false;

  private static final String LEASE_KEY = "LEASE";
  private static final int LEASE_TIMEOUT_SEC = 200;
  protected static final int DEFAULT_EXP = 0;

  protected InetSocketAddress serverAddress;
  protected String version;
  protected boolean isAscii;
  protected boolean requireSasl;

  protected MemcachedClient client;

  public SpyMemcachedBaseTest(String server, int port, String version, boolean isAscii) {
    this(server, port, version, isAscii, false);
  }

  public SpyMemcachedBaseTest(
      String server, int port, String version, boolean isAscii, boolean requireSasl) {
    this.serverAddress = new InetSocketAddress(server, port);
    this.version = version;
    this.isAscii = isAscii;
    this.requireSasl = requireSasl;
  }

  void setUp() throws Exception {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
    // Disable SASL since we are not going to support it.
    // if (requireSasl) {
    //   if (isAscii) {
    //     throw new IllegalArgumentException("Binary protocal required for SASL");
    //   }
    //   synchronized (SASL_INITIALIZAQTION_LOCK) {
    //     if (!SASL_INITIALIZED) {
    //       com.google.cloud.memcache.Authentication.initialize();
    //       SASL_INITIALIZED = true;
    //     }
    //   }
    //   cfb.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);
    //   cfb.setAuthDescriptor(
    //       new AuthDescriptor(
    //           new String[] {com.google.cloud.memcache.Authentication.MECHANISM}, null));
    // }
    // cfb.setTranscoder(new SpymemcachedSerializingTranscoder());
    cfb.setProtocol(isAscii ? Protocol.TEXT : Protocol.BINARY);
    ArrayList<InetSocketAddress> servers = new ArrayList<>();
    servers.add(serverAddress);
    client = new MemcachedClient(cfb.build(), servers);
  }

  void tearDown() throws Exception {
    client.shutdown();
  }

  protected void expectTrue(boolean condition, String template, Object... values) {
    System.out.print(isAscii ? "ascii " : "binary ");
    System.out.print(String.format(template, values));
    if (!condition) {
      testPassed = false;
      System.out.println("\t[FAIL]");
    } else {
      System.out.println("\t[pass]");
    }
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
}
