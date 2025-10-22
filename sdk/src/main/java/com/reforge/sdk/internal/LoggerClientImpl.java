package com.reforge.sdk.internal;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.LogLevel;
import com.reforge.sdk.LoggerClient;
import com.reforge.sdk.context.Context;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerClientImpl implements LoggerClient {

  private static final Logger LOG = LoggerFactory.getLogger(LoggerClientImpl.class);
  private static final String LOGGING_CONTEXT_NAME = "reforge-sdk-logging";
  private static final String LANG_KEY = "lang";
  private static final String LOGGER_PATH_KEY = "logger-path";
  private static final String JAVA_VALUE = "java";

  private final ConfigClient configClient;
  private final String loggerKey;

  public LoggerClientImpl(ConfigClient configClient, @Nullable String loggerKey) {
    this.configClient = configClient;
    this.loggerKey = loggerKey;
  }

  @Override
  public LogLevel getLogLevel(String loggerName) {
    if (loggerKey == null || loggerKey.isEmpty()) {
      LOG.debug("No logger key configured, returning default level DEBUG");
      return LogLevel.DEBUG;
    }

    try {
      Context loggingContext = Context
        .newBuilder(LOGGING_CONTEXT_NAME)
        .put(LANG_KEY, JAVA_VALUE)
        .put(LOGGER_PATH_KEY, loggerName)
        .build();

      Optional<Prefab.ConfigValue> configValueMaybe = configClient.get(
        loggerKey,
        loggingContext
      );

      if (!configValueMaybe.isPresent()) {
        LOG.debug(
          "No log level configuration found for key '{}' and logger '{}', returning default level DEBUG",
          loggerKey,
          loggerName
        );
        return LogLevel.DEBUG;
      }

      Prefab.ConfigValue configValue = configValueMaybe.get();

      if (!configValue.hasLogLevel()) {
        LOG.warn(
          "Config value for key '{}' is not a log level (type: {}), returning default level DEBUG",
          loggerKey,
          configValue.getTypeCase()
        );
        return LogLevel.DEBUG;
      }

      Prefab.LogLevel protobufLogLevel = configValue.getLogLevel();
      LogLevel result = LogLevel.fromProtobuf(protobufLogLevel);

      LOG.debug(
        "Retrieved log level {} for logger '{}' from key '{}'",
        result,
        loggerName,
        loggerKey
      );

      return result;
    } catch (Exception e) {
      LOG.debug(
        "Error retrieving log level for logger '{}' from key '{}', returning default level DEBUG",
        loggerName,
        loggerKey,
        e
      );
      return LogLevel.DEBUG;
    }
  }
}
