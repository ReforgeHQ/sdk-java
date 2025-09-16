package com.reforge.sdk.internal;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Optional;
import javax.annotation.Nullable;

public class FeatureFlagClientImpl extends AbstractFeatureFlagResolverImpl {

  private final ConfigClient configClient;

  public FeatureFlagClientImpl(ConfigClient configClient) {
    this.configClient = configClient;
  }

  protected Optional<Prefab.ConfigValue> getConfigValue(
    String feature,
    @Nullable ContextSetReadable prefabContext
  ) {
    return configClient.get(feature, prefabContext);
  }
}
