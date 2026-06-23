package util;

import main.ExpressoLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

public class TimeUtil {
  /* Timer part */
  private double total = 0;
  private int n = 0;
  private long start;

  public void begin() {
    start = System.nanoTime();
  }

  public void begin(ExpressoLogger.LEVEL level, String msg) {
    if (!msg.equals("")) msg += " ";
    ExpressoLogger.logTimeUse(level, msg);
    start = System.nanoTime();
  }

  public double end() {
    double runtime = (System.nanoTime() - start) / 1e9;
    total += runtime;
    n++;
    return runtime;
  }

  public double end(ExpressoLogger.LEVEL level, String msg) {
    double runtime = (System.nanoTime() - start) / 1e9;
    total += runtime;
    n++;
    if (!msg.equals("")) msg += " ";
    ExpressoLogger.logTimeUse(level, msg + runtime + "s");
    return runtime;
  }

  public void logStatistic(ExpressoLogger.LEVEL level, String msg) {
    String str =
        String.format(
            "%stotal runtime: %.2fs, number: %d, avg runtime: %.2fs",
            msg.isEmpty() ? "" : msg + ", ", total, n, total / n);
    ExpressoLogger.logTimeUse(level, str);
  }

  public double getTotal() {
    return total;
  }

  /* Date part */

  public static String getDate() {
    Date date = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return simpleDateFormat.format(date);
  }

  public static String getDate(String format) {
    Date date = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
    return simpleDateFormat.format(date);
  }

  /* static methods */

  private static final Stack<Long> startTimes = new Stack<>();
  private static long stopTime;

  /** Please use the method with {@link TimeUtil#stop()} in pair! */
  public static void start() {
    startTimes.push(System.nanoTime());
  }

  /** Please use the method with {@link TimeUtil#start()} in pair! */
  public static void stop() {
    stopTime = System.nanoTime();
    long startTime = startTimes.pop();
    System.out.printf("%fs\n", (stopTime - startTime) / 1e9);
  }

  /** Please use the method with {@link TimeUtil#start()} in pair! */
  public static void stop(String msg) {
    stopTime = System.nanoTime();
    long startTime = startTimes.pop();
    System.out.printf("%s: %fs\n", msg, (stopTime - startTime) / 1e9);
  }

  /** Please use the method with {@link TimeUtil#start()} in pair! */
  public static boolean stop(String msg, double limit) {
    stopTime = System.nanoTime();
    long startTime = startTimes.pop();
    double elapsed = (stopTime - startTime) / 1e9;
    if (elapsed >= limit) {
      System.out.printf("%s: %fs\n", msg, elapsed);
    }
    return elapsed >= limit;
  }
}
