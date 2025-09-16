package com.reforge.sdk.value;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.config.ConfigValueUtils;
import java.util.Optional;

public class LiveObject extends AbstractLiveValue<Object> {

  public LiveObject(ConfigClient configClient, String key) {
    super(configClient, key);
  }

  @Override
  public Optional<Object> resolve(Prefab.ConfigValue value) {
    return ConfigValueUtils.asObject(value);
  }
}
