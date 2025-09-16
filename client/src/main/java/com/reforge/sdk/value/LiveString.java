package com.reforge.sdk.value;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveString extends AbstractLiveValue<String> {

  private static final Logger LOG = LoggerFactory.getLogger(LiveString.class);

  public LiveString(ConfigClient configClient, String key) {
    super(configClient, key);
  }

  @Override
  public Optional<String> resolve(Prefab.ConfigValue value) {
    if (value.hasString()) {
      return Optional.of(value.getString());
    } else {
      LOG.warn(
        String.format(
          "Config value for key '%s' used as a string does not have a string value, will treat as empty",
          key
        )
      );
      return Optional.empty();
    }
  }
}
