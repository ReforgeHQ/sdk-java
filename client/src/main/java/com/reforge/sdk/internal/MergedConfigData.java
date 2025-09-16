package com.reforge.sdk.internal;

import com.reforge.sdk.config.ConfigElement;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Map;

public class MergedConfigData {

  private final Map<String, ConfigElement> configs;
  private final long envId;
  private final ContextSetReadable globalContextSet;
  private final ContextSetReadable configIncludedContextSet;

  MergedConfigData(
    Map<String, ConfigElement> configs,
    long envId,
    ContextSetReadable globalContextSet,
    ContextSetReadable configIncludedContextSet
  ) {
    this.configs = configs;
    this.envId = envId;
    this.globalContextSet = globalContextSet;
    this.configIncludedContextSet = configIncludedContextSet;
  }

  public Map<String, ConfigElement> getConfigs() {
    return configs;
  }

  public ContextSetReadable getConfigIncludedContext() {
    return configIncludedContextSet;
  }

  public long getEnvId() {
    return envId;
  }

  public ContextSetReadable getGlobalContextSet() {
    return globalContextSet;
  }
}
