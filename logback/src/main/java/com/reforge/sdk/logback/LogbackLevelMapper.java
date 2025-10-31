package com.reforge.sdk.logback;

import ch.qos.logback.classic.Level;
import com.reforge.sdk.LogLevel;

class LogbackLevelMapper {

  static Level toLogbackLevel(LogLevel reforgeLevel) {
    switch (reforgeLevel) {
      case FATAL:
        return Level.ERROR; // Logback doesn't have FATAL, map to ERROR
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
