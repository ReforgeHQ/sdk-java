package com.reforge.sdk.internal;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.config.ConfigChangeEvent;
import java.util.Optional;

public class ConfigStoreConfigValueDeltaCalculator
  extends AbstractConfigStoreDeltaCalculator<Prefab.Config, ConfigChangeEvent> {

  @Override
  ConfigChangeEvent createEvent(
    String name,
    Optional<Prefab.Config> oldValue,
    Optional<Prefab.Config> newValue
  ) {
    return new ConfigChangeEvent(name, oldValue, newValue);
  }
}
