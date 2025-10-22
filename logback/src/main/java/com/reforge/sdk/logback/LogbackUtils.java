package com.reforge.sdk.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class LogbackUtils {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogbackUtils.class);

  static void installTurboFilter(TurboFilter turboFilter) {
    ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
    if (iLoggerFactory instanceof LoggerContext) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      loggerContext.addTurboFilter(turboFilter);
    } else {
      LOG.error(
        "Unable to install {} - LoggerFactory is not a Logback LoggerContext. Current factory: {}",
        turboFilter.getClass().getSimpleName(),
        iLoggerFactory.getClass().getName()
      );
    }
  }
}
