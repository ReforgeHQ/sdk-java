package com.reforge.sdk;

import cloud.prefab.domain.Prefab;

/**
 * Log level enum that wraps the underlying Prefab.LogLevel protobuf enum.
 * Used to configure logging levels for different loggers in your application.
 */
public enum LogLevel {
  TRACE(Prefab.LogLevel.TRACE),
  DEBUG(Prefab.LogLevel.DEBUG),
  INFO(Prefab.LogLevel.INFO),
  WARN(Prefab.LogLevel.WARN),
  ERROR(Prefab.LogLevel.ERROR),
  FATAL(Prefab.LogLevel.FATAL);

  private final Prefab.LogLevel protobufLogLevel;

  LogLevel(Prefab.LogLevel protobufLogLevel) {
    this.protobufLogLevel = protobufLogLevel;
  }

  /**
   * @return the underlying protobuf LogLevel
   */
  public Prefab.LogLevel toProtobuf() {
    return protobufLogLevel;
  }

  /**
   * Converts a protobuf LogLevel to a Java LogLevel enum
   * @param protobufLogLevel the protobuf log level
   * @return the corresponding Java LogLevel, or DEBUG if the protobuf value is NOT_SET_LOG_LEVEL or unrecognized
   */
  public static LogLevel fromProtobuf(Prefab.LogLevel protobufLogLevel) {
    switch (protobufLogLevel) {
      case TRACE:
        return TRACE;
      case DEBUG:
        return DEBUG;
      case INFO:
        return INFO;
      case WARN:
        return WARN;
      case ERROR:
        return ERROR;
      case FATAL:
        return FATAL;
      case NOT_SET_LOG_LEVEL:
      case UNRECOGNIZED:
      default:
        return DEBUG;
    }
  }
}
