package main;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

import java.util.Stack;

public class ExpressoLogger {
  public enum LEVEL {
    DEBUG,
    INFO,
    WARN,
    ERROR
  }

  static boolean debugMemUse = true;
  static boolean debugCPExe = true;
  static boolean debugWorkFlow = true;
  static boolean debugTimeUse = true;

  static Stack<String> contexts = new Stack<>();

  public static void pushContext(String context) {
    contexts.push('[' + context + ']');
  }

  public static void popContext() {
    if (!contexts.empty()) contexts.pop();
  }

  private static final Logger logger = Logger.getLogger(ExpressoLogger.class);

  public static void setAppender(Appender appender) {
    logger.removeAllAppenders();
    logger.addAppender(appender);
  }

  public static void logMemUse(LEVEL level, String msg) {
    if (debugMemUse) {
      log(level, msg);
    }
  }

  public static void logCPExe(LEVEL level, String msg) {
    if (debugCPExe) {
      log(level, msg);
    }
  }

  public static void logWorkFlow(LEVEL level, String msg) {
    if (debugWorkFlow) {
      log(level, msg);
    }
  }

  public static void logTimeUse(LEVEL level, String msg) {
    if (debugTimeUse) {
      log(level, msg);
    }
  }

  public static void log(LEVEL level, String msg) {
    if (!contexts.empty()) msg = String.join(" ", contexts) + " " + msg;
    switch (level) {
      case DEBUG:
        logger.debug(msg);
        break;
      case INFO:
        logger.info(msg);
        break;
      case WARN:
        logger.warn(msg);
        break;
      case ERROR:
        logger.error(msg);
        break;
    }
  }
}
