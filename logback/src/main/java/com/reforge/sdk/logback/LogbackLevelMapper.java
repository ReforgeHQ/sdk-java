package com.reforge.sdk.logback;

import ch.qos.logback.classic.Level;
import com.reforge.sdk.LogLevel;
import java.util.HashMap;
import java.util.Map;

class LogbackLevelMapper {

  static final Map<LogLevel, Level> LEVEL_MAP;
  static final Map<Level, LogLevel> REVERSE_LEVEL_MAP;

  static {
    Map<LogLevel, Level> levelMap = new HashMap<>();
    levelMap.put(LogLevel.FATAL, Level.ERROR);
    levelMap.put(LogLevel.ERROR, Level.ERROR);
    levelMap.put(LogLevel.WARN, Level.WARN);
    levelMap.put(LogLevel.INFO, Level.INFO);
    levelMap.put(LogLevel.DEBUG, Level.DEBUG);
    levelMap.put(LogLevel.TRACE, Level.TRACE);
    LEVEL_MAP = Map.copyOf(levelMap);

    Map<Level, LogLevel> reverseLevelMap = new HashMap<>();
    reverseLevelMap.put(Level.ERROR, LogLevel.ERROR);
    reverseLevelMap.put(Level.WARN, LogLevel.WARN);
    reverseLevelMap.put(Level.INFO, LogLevel.INFO);
    reverseLevelMap.put(Level.DEBUG, LogLevel.DEBUG);
    reverseLevelMap.put(Level.TRACE, LogLevel.TRACE);
    REVERSE_LEVEL_MAP = Map.copyOf(reverseLevelMap);
  }
}
