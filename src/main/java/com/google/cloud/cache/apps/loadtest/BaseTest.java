package com.google.cloud.cache.apps.loadtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

abstract class BaseTest {

  protected static final java.util.Random random = new java.util.Random();

  protected boolean testPassed = true;
  protected StringBuffer result = new StringBuffer();
  private volatile boolean stop = false;

  BaseTest() {}

  public final void stopTest() {
    this.stop = true;
  }

  public final boolean testStopped() {
    return stop;
  }

  protected void expectTrue(boolean condition, String template, Object... values) {
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
    boolean condition = false;
    if (expected == null) {
      condition = (expected == actual);
    } else if (expected instanceof byte[]) {
      condition = Arrays.equals((byte[]) expected, (byte[]) actual);
    } else {
      condition = expected.equals(actual);
    }
    if (!condition) {
      result.append(String.format(" actual=%s ", actual));
    }
    expectTrue(condition, template, values);
  }

  public boolean isTestPassed() {
    return testPassed;
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

  public String getResult() {
    return result.toString();
  }
}
