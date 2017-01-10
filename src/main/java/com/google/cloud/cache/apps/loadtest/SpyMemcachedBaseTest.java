package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.transcoders.SpymemcachedSerializingTranscoder;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;

abstract class SpyMemcachedBaseTest extends BaseTest {

  private static final String LEASE_KEY = "LEASE";
  private static final int LEASE_TIMEOUT_SEC = 200;
  protected static final int DEFAULT_EXP = 0;

  protected InetSocketAddress serverAddress;
  protected String version;
  protected boolean isAscii;

  protected MemcachedClient client;

  public SpyMemcachedBaseTest(String server, int port, String version, boolean isAscii) {
    this.serverAddress = new InetSocketAddress(server, port);
    this.version = version;
    this.isAscii = isAscii;
  }

  void setUp() throws Exception {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
    cfb.setTranscoder(new SpymemcachedSerializingTranscoder());
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
    super.expectTrue(condition, template, values);
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
