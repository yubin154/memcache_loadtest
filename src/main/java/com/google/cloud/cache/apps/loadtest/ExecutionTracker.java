package com.google.cloud.cache.apps.loadtest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class ExecutionTracker {

  private static final ExecutionTracker me = new ExecutionTracker();

  private final ExecutorService executor = Executors.newCachedThreadPool();
  private AtomicInteger qpsCounter = new AtomicInteger(0);
  private AtomicInteger errorCounter = new AtomicInteger(0);

  private ExecutionTracker() {}

  public int getAndResetQps() {
    return qpsCounter.getAndSet(0);
  }

  public void incrementQps() {
    qpsCounter.incrementAndGet();
  }

  public int getAndResetErrorCount() {
    return errorCounter.getAndSet(0);
  }

  public void incrementErrorCount() {
    errorCounter.incrementAndGet();
  }

  public ExecutorService getExecutorService() {
    return executor;
  }

  public static final ExecutionTracker getInstance() {
    return me;
  }

  public static final ExecutionTracker newInstance() {
    return new ExecutionTracker();
  }

}
