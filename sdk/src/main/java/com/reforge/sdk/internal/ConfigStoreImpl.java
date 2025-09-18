package com.reforge.sdk.internal;

import com.reforge.sdk.ConfigStore;
import com.reforge.sdk.config.ConfigElement;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigStoreImpl implements ConfigStore {

  private final AtomicReference<MergedConfigData> data = new AtomicReference<>(
    new MergedConfigData(Map.of(), 0, ContextSetReadable.EMPTY, ContextSetReadable.EMPTY)
  );

  @Override
  public Collection<String> getKeys() {
    return data.get().getConfigs().keySet();
  }

  public Set<Map.Entry<String, ConfigElement>> entrySet() {
    return data.get().getConfigs().entrySet();
  }

  @Override
  public Collection<ConfigElement> getElements() {
    return data.get().getConfigs().values();
  }

  public void set(MergedConfigData mergedConfigData) {
    data.set(mergedConfigData);
  }

  @Override
  public ConfigElement getElement(String key) {
    return data.get().getConfigs().get(key);
  }

  @Override
  public boolean containsKey(String key) {
    return data.get().getConfigs().containsKey(key);
  }

  @Override
  public long getProjectEnvironmentId() {
    return data.get().getEnvId();
  }

  @Override
  public ContextSetReadable getConfigIncludedContext() {
    return data.get().getConfigIncludedContext();
  }

  @Override
  public ContextSetReadable getGlobalContext() {
    return data.get().getGlobalContextSet();
  }
}
