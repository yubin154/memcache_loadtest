package com.google.cloud.cache.apps.loadtest;

import static com.google.common.collect.BoundType.OPEN;

import com.google.common.collect.Range;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

final class MemcacheValues {
  private static final String ASCII_LETTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  static String random(Range<Integer> sizeRange) {
    int min = sizeRange.lowerEndpoint();
    int max = sizeRange.upperEndpoint();
    if (sizeRange.lowerBoundType() == OPEN) {
      min += 1;
    }
    if (sizeRange.upperBoundType() == OPEN) {
      max -= 1;
    }

    int n = ThreadLocalRandom.current().nextInt(max - min + 1);
    int length = n + max - min + 1;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; ++i) {
      builder.append(randomAsciiLetter());
    }
    return builder.toString();
  }

  private static char randomAsciiLetter() {
    return ASCII_LETTERS.charAt(ThreadLocalRandom.current().nextInt(ASCII_LETTERS.length()));
  }

  public static String randomKey() {
    return UUID.randomUUID().toString();
  }

  private MemcacheValues() {}
}
