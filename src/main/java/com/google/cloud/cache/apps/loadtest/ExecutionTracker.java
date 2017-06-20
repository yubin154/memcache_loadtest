package com.google.cloud.cache.apps.loadtest;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class ExecutionTracker {

  private static final ExecutorService executor = Executors.newCachedThreadPool();

  private AtomicInteger qpsCounter = new AtomicInteger(0);
  private AtomicInteger errorCounter = new AtomicInteger(0);
  private AtomicInteger missCounter = new AtomicInteger(0);
  private List<Integer> qpsMeasures = new ArrayList<>();
  private List<Integer> errorMeasures = new ArrayList<>();
  private List<Integer> missMeasures = new ArrayList<>();

  private ExecutionTracker() {}

  public int getAndResetQps() {
    int qpsMeasure = qpsCounter.getAndSet(0);
    qpsMeasures.add(qpsMeasure);
    return qpsMeasure;
  }

  public void incrementQps() {
    qpsCounter.incrementAndGet();
  }

  public int getAndResetErrorCount() {
    int errorMeasure = errorCounter.getAndSet(0);
    errorMeasures.add(errorMeasure);
    return errorMeasure;
  }

  public void incrementErrorCount() {
    errorCounter.incrementAndGet();
  }

  public int getAndResetMissCount() {
    int missMeasure = missCounter.getAndSet(0);
    missMeasures.add(missMeasure);
    return missMeasure;
  }

  public void incrementMissCount() {
    missCounter.incrementAndGet();
  }

  public double averageQps() {
    OptionalDouble average = qpsMeasures.stream().mapToDouble(i -> i).average();
    return average.isPresent() ? average.getAsDouble() : 0;
  }

  public double averageErrors() {
    OptionalDouble average = errorMeasures.stream().mapToDouble(i -> i).average();
    return average.isPresent() ? average.getAsDouble() : 0;
  }

  public double averageMisses() {
    OptionalDouble average = missMeasures.stream().mapToDouble(i -> i).average();
    return average.isPresent() ? average.getAsDouble() : 0;
  }

  public static final ExecutionTracker newInstance() {
    return new ExecutionTracker();
  }

  public static ExecutorService getExecutorService() {
    return executor;
  }

}
