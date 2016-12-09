package com.google.cloud.cache.apps.loadtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;

/** */
public final class SpymemcachedUtil {

  private static final String DEFAULT_SERVER = "169.254.10.1";
  private static final SpymemcachedUtil me = new SpymemcachedUtil();

  public static MemcachedClient binaryClient() {
    return me.binaryClient;
  }

  public static MemcachedClient textClient() {
    return me.textClient;
  }

  public static MemcachedClient binaryClient(String server) throws IOException {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
    cfb.setProtocol(Protocol.BINARY);
    ArrayList<InetSocketAddress> servers = new ArrayList<>();
    servers.add(new InetSocketAddress(server, 11211));
    return new MemcachedClient(cfb.build(), servers);
  }

  public static MemcachedClient textClient(String server) throws IOException {
    ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
    cfb.setProtocol(Protocol.TEXT);
    ArrayList<InetSocketAddress> servers = new ArrayList<>();
    servers.add(new InetSocketAddress(server, 11211));
    return new MemcachedClient(cfb.build(), servers);
  }

  private MemcachedClient binaryClient;
  private MemcachedClient textClient;

  private SpymemcachedUtil() {
    try {
      ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
      cfb.setProtocol(Protocol.BINARY);
      ArrayList<InetSocketAddress> servers = new ArrayList<>();
      servers.add(new InetSocketAddress(DEFAULT_SERVER, 11211));
      binaryClient = new MemcachedClient(cfb.build(), servers);
      ConnectionFactoryBuilder textCfb = new ConnectionFactoryBuilder();
      textCfb.setProtocol(Protocol.TEXT);
      textClient = new MemcachedClient(textCfb.build(), servers);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
