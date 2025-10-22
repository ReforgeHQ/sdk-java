package com.reforge.sdk.log4j2;

import com.reforge.sdk.LogLevel;
import com.reforge.sdk.LoggerClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * Log4j2 filter that retrieves log levels from Reforge configuration.
 *
 * <p>This filter intercepts logging calls and dynamically determines whether they should
 * be logged based on the log level configuration from Reforge, allowing centralized
 * and real-time control over application logging levels.
 *
 * <p>To install this filter, call:
 * <pre>{@code
 * Sdk sdk = new Sdk(new Options());
 * ReforgeLog4j2Filter.install(sdk.loggerClient());
 * }</pre>
 *
 * <p><b>Important:</b> Install the filter after Log4j2 is initialized. Any dynamic
 * reconfiguration of Log4j2 will remove this filter and it will need to be reinstalled.
 */
public class ReforgeLog4j2Filter extends AbstractFilter {

  private final ThreadLocal<Boolean> recursionCheck = ThreadLocal.withInitial(() -> false
  );
  private final LoggerClient loggerClient;

  /**
   * Installs ReforgeLog4j2Filter at the LoggerContext level.
   * Call only after Log4j2 is initialized.
   *
   * <p><b>Note:</b> Any dynamic reconfiguration of Log4j2 will remove this filter.
   *
   * @param loggerClient the LoggerClient to use for retrieving log levels
   * @throws IllegalStateException if Log4j2 Core is not being used
   */
  public static void install(LoggerClient loggerClient) {
    org.apache.logging.log4j.spi.LoggerContext ctx = LogManager.getContext(false);

    if (!(ctx instanceof LoggerContext)) {
      throw new IllegalStateException(
        "Cannot install ReforgeLog4j2Filter - LoggerContext is not Log4j2 Core. " +
        "Found: " +
        ctx.getClass().getName() +
        ". " +
        "Make sure log4j-core is on your classpath and properly configured."
      );
    }

    LoggerContext loggerContext = (LoggerContext) ctx;
    loggerContext.addFilter(new ReforgeLog4j2Filter(loggerClient));
    loggerContext.updateLoggers();
  }

  public ReforgeLog4j2Filter(final LoggerClient loggerClient) {
    this.loggerClient = loggerClient;
  }

  Result decide(final String loggerName, final Level level) {
    if (recursionCheck.get()) {
      return Result.NEUTRAL;
    }

    try {
      recursionCheck.set(true);

      LogLevel reforgeLogLevel = loggerClient.getLogLevel(loggerName);
      Level calculatedMinLogLevelToAccept = Log4jLevelMapper.toLog4jLevel(
        reforgeLogLevel
      );

      if (level.isMoreSpecificThan(calculatedMinLogLevelToAccept)) {
        return Result.ACCEPT;
      }
      return Result.DENY;
    } catch (Exception e) {
      // If there's any error, fall back to neutral to avoid breaking logging
      return Result.NEUTRAL;
    } finally {
      recursionCheck.set(false);
    }
  }

  @Override
  public Result filter(final LogEvent event) {
    return decide(event.getLoggerName(), event.getLevel());
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final Message msg,
    final Throwable t
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final Object msg,
    final Throwable t
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object... params
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4,
    final Object p5
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4,
    final Object p5,
    final Object p6
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4,
    final Object p5,
    final Object p6,
    final Object p7
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4,
    final Object p5,
    final Object p6,
    final Object p7,
    final Object p8
  ) {
    return decide(logger.getName(), level);
  }

  @Override
  public Result filter(
    final Logger logger,
    final Level level,
    final Marker marker,
    final String msg,
    final Object p0,
    final Object p1,
    final Object p2,
    final Object p3,
    final Object p4,
    final Object p5,
    final Object p6,
    final Object p7,
    final Object p8,
    final Object p9
  ) {
    return decide(logger.getName(), level);
  }
}
