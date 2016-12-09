package com.google.cloud.cache.apps.loadtest;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.ServletRequest;

final class RequestReader {
  private static final int DEFAULT_BATCH_SIZE = 10;
  private static final int DEFAULT_KEY_SPACE_SIZE = 1000;
  private static final int DEFAULT_ITERATION_COUNT = 1;
  private static final int DEFAULT_VALUE_SIZE = 1024;
  private static final int DEFAULT_DURATION_SEC = 0;
  private static final int DEFAULT_FRONTEND_QPS = 1;
  private static final int DEFAULT_CLIENT_SIZE = 1;

  private static final double SIZE_TOLERANCE = 0.1;

  public static RequestReader create(ServletRequest request) {
    return new RequestReader(request);
  }

  private final ServletRequest request;

  private RequestReader(ServletRequest request) {
    this.request = request;
  }

  /** Gets the (possibly random) key to use for the given request. */
  String readKey() {
    String key = request.getParameter("key");
    if (key == null) {
      return randomKey(readKeySpaceSize());
    }
    return key;
  }

  ImmutableList<String> readKeys() {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    int keySpaceSize = readKeySpaceSize();
    int batchSize = readBatchSize();
    for (int i = 0; i < batchSize; ++i) {
      builder.add(randomKey(keySpaceSize));
    }
    return builder.build();
  }

  int readIterationCount() {
    return readInt("times", DEFAULT_ITERATION_COUNT);
  }

  boolean isMemcacheg() {
    return "g".equalsIgnoreCase(readStr("client"));
  }

  /** Gets min and max value sizes from request; use defaults if not specified. */
  Range<Integer> readValueSizeRange() {
    int valueSize = readInt("value_size", DEFAULT_VALUE_SIZE);
    return Range.closed(
        (int) ceil(valueSize * (1 - SIZE_TOLERANCE)),
        (int) floor(valueSize * (1 + SIZE_TOLERANCE)));
  }

  int readDurationSec() {
    return readInt("duration_sec", DEFAULT_DURATION_SEC);
  }

  int readFrontendQps() {
    return readInt("frontend_qps", DEFAULT_FRONTEND_QPS);
  }

  int readClientSize() {
    return readInt("client_size", DEFAULT_CLIENT_SIZE);
  }

  /** Generate a random key. * */
  private static String randomKey(int keySpaceSize) {
    return "Key_" + ThreadLocalRandom.current().nextInt(keySpaceSize);
  }

  private int readKeySpaceSize() {
    return readInt("key_space_size", DEFAULT_KEY_SPACE_SIZE);
  }

  private int readBatchSize() {
    return readInt("batch_size", DEFAULT_BATCH_SIZE);
  }

  private String readStr(String parameter) {
    return request.getParameter(parameter);
  }

  private int readInt(String parameter, int defaultValue) {
    String s = request.getParameter(parameter);
    if (s == null) {
      return defaultValue;
    }
    Integer value = Ints.tryParse(s);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
