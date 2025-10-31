package com.reforge.sdk;

/**
 * Client for retrieving dynamically configured log levels from Reforge.
 * Log levels can be configured per logger path with context-aware targeting.
 */
public interface LoggerClient {
  /**
   * Get the configured log level for a specific logger.
   *
   * This evaluates the configured logger key (from Options) with a context containing:
   * - "reforge-sdk-logging.lang": "java"
   * - "reforge-sdk-logging.logger-path": the provided loggerName
   *
   * The config must be of type LOG_LEVEL_V2.
   *
   * @param loggerName the name or path of the logger (e.g., "com.example.MyClass" or "com.example")
   * @return the configured LogLevel for this logger, or DEBUG if no configuration is found
   */
  LogLevel getLogLevel(String loggerName);
}
