package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.LogLevel;
import com.reforge.sdk.config.ConfigValueUtils;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoggerClientImplTest {

  @Mock
  ConfigClient configClient;

  @Nested
  class WithLoggerKey {

    @Test
    void getLogLevelReturnsConfiguredLevel() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.INFO)));

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.INFO);
    }

    @Test
    void getLogLevelReturnsDebugWhenNoConfigFound() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.empty());

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void getLogLevelReturnsDebugWhenConfigIsWrongType() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from("not a log level")));

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void getLogLevelPassesCorrectContext() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      ArgumentCaptor<ContextSetReadable> contextCaptor = ArgumentCaptor.forClass(
        ContextSetReadable.class
      );
      when(configClient.get(eq("log-levels.default"), contextCaptor.capture()))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.WARN)));

      loggerClient.getLogLevel("com.example.MyClass");

      ContextSetReadable capturedContext = contextCaptor.getValue();
      Optional<Context> loggingContext = capturedContext.getByName("reforge-sdk-logging");

      assertThat(loggingContext).isPresent();
      assertThat(loggingContext.get().getProperties())
        .containsEntry("lang", ConfigValueUtils.from("java"))
        .containsEntry("logger-path", ConfigValueUtils.from("com.example.MyClass"));
    }

    @Test
    void getLogLevelHandlesAllLogLevels() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      // Test TRACE
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.TRACE)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.TRACE);

      // Test DEBUG
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.DEBUG)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.DEBUG);

      // Test INFO
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.INFO)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.INFO);

      // Test WARN
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.WARN)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.WARN);

      // Test ERROR
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.ERROR)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.ERROR);

      // Test FATAL
      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.FATAL)));
      assertThat(loggerClient.getLogLevel("test")).isEqualTo(LogLevel.FATAL);
    }

    @Test
    void getLogLevelHandlesNotSetLogLevel() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenReturn(
          Optional.of(ConfigValueUtils.from(Prefab.LogLevel.NOT_SET_LOG_LEVEL))
        );

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void getLogLevelHandlesCustomLoggerKey() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "custom.logger.config"
      );

      when(configClient.get(eq("custom.logger.config"), any(ContextSetReadable.class)))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.ERROR)));

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.ERROR);
      verify(configClient).get(eq("custom.logger.config"), any(ContextSetReadable.class));
    }
  }

  @Nested
  class WithoutLoggerKey {

    @Test
    void getLogLevelReturnsDebugWhenLoggerKeyIsNull() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(configClient, null);

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void getLogLevelReturnsDebugWhenLoggerKeyIsEmpty() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(configClient, "");

      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void getLogLevelHandlesExceptionFromConfigClient() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      when(configClient.get(eq("log-levels.default"), any(ContextSetReadable.class)))
        .thenThrow(new RuntimeException("Config error"));

      // Should not throw, should return default
      LogLevel result = loggerClient.getLogLevel("com.example.MyClass");

      assertThat(result).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void getLogLevelHandlesDifferentLoggerPaths() {
      LoggerClientImpl loggerClient = new LoggerClientImpl(
        configClient,
        "log-levels.default"
      );

      ArgumentCaptor<ContextSetReadable> contextCaptor = ArgumentCaptor.forClass(
        ContextSetReadable.class
      );
      when(configClient.get(eq("log-levels.default"), contextCaptor.capture()))
        .thenReturn(Optional.of(ConfigValueUtils.from(Prefab.LogLevel.INFO)));

      // Test package-level logger
      loggerClient.getLogLevel("com.example");
      ContextSetReadable context1 = contextCaptor.getValue();
      assertThat(context1.getByName("reforge-sdk-logging").get().getProperties())
        .containsEntry("logger-path", ConfigValueUtils.from("com.example"));

      // Test class-level logger
      loggerClient.getLogLevel("com.example.MyClass");
      ContextSetReadable context2 = contextCaptor.getValue();
      assertThat(context2.getByName("reforge-sdk-logging").get().getProperties())
        .containsEntry("logger-path", ConfigValueUtils.from("com.example.MyClass"));

      // Test root logger
      loggerClient.getLogLevel("ROOT");
      ContextSetReadable context3 = contextCaptor.getValue();
      assertThat(context3.getByName("reforge-sdk-logging").get().getProperties())
        .containsEntry("logger-path", ConfigValueUtils.from("ROOT"));
    }
  }
}
