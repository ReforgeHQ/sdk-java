package com.reforge.sdk.log4j2;

import com.reforge.sdk.LogLevel;
import org.apache.logging.log4j.Level;

class Log4jLevelMapper {

  static Level toLog4jLevel(LogLevel reforgeLevel) {
    switch (reforgeLevel) {
      case FATAL:
        return Level.FATAL;
      case ERROR:
        return Level.ERROR;
      case WARN:
        return Level.WARN;
      case INFO:
        return Level.INFO;
      case DEBUG:
        return Level.DEBUG;
      case TRACE:
        return Level.TRACE;
      default:
        return Level.DEBUG;
    }
  }
}
