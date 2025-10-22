package com.reforge.sdk.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.reforge.sdk.LogLevel;
import com.reforge.sdk.LoggerClient;
import org.slf4j.Marker;

public abstract class BaseTurboFilter extends TurboFilter {

  protected final LoggerClient loggerClient;
  private final ThreadLocal<Boolean> recursionCheck = ThreadLocal.withInitial(() -> false
  );

  BaseTurboFilter(LoggerClient loggerClient) {
    this.loggerClient = loggerClient;
  }

  abstract LogLevel getLogLevel(Logger logger, Level level);

  @Override
  public FilterReply decide(
    Marker marker,
    Logger logger,
    Level level,
    String s,
    Object[] objects,
    Throwable throwable
  ) {
    if (recursionCheck.get()) {
      return FilterReply.NEUTRAL;
    }

    try {
      recursionCheck.set(true);
      LogLevel reforgeLogLevel = getLogLevel(logger, level);

      Level calculatedMinLogLevelToAccept = LogbackLevelMapper.LEVEL_MAP.get(
        reforgeLogLevel
      );

      if (calculatedMinLogLevelToAccept == null) {
        return FilterReply.NEUTRAL;
      }

      if (level.isGreaterOrEqual(calculatedMinLogLevelToAccept)) {
        return FilterReply.ACCEPT;
      }
      return FilterReply.DENY;
    } catch (Exception e) {
      // If there's any error, fall back to neutral to avoid breaking logging
      return FilterReply.NEUTRAL;
    } finally {
      recursionCheck.set(false);
    }
  }
}
