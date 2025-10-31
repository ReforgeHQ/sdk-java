package com.reforge.sdk.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.reforge.sdk.LogLevel;
import com.reforge.sdk.LoggerClient;

/**
 * Logback TurboFilter that retrieves log levels from Reforge configuration.
 *
 * This filter intercepts logging calls and dynamically determines whether they should
 * be logged based on the log level configuration from Reforge, allowing centralized
 * and real-time control over application logging levels.
 *
 * <p>To install this filter, call:
 * <pre>{@code
 * Sdk sdk = new Sdk(new Options());
 * ReforgeLogbackTurboFilter.install(sdk.loggerClient());
 * }</pre>
 */
public class ReforgeLogbackTurboFilter extends BaseTurboFilter {

  ReforgeLogbackTurboFilter(LoggerClient loggerClient) {
    super(loggerClient);
  }

  /**
   * Installs the Reforge turbo filter into the Logback logging system.
   *
   * @param loggerClient the LoggerClient to use for retrieving log levels
   * @throws IllegalStateException if Logback is not being used as the SLF4J implementation
   */
  public static void install(LoggerClient loggerClient) {
    LogbackUtils.installTurboFilter(new ReforgeLogbackTurboFilter(loggerClient));
  }

  @Override
  LogLevel getLogLevel(Logger logger, Level level) {
    return loggerClient.getLogLevel(logger.getName());
  }
}
