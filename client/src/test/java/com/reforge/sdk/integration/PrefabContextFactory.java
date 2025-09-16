package com.reforge.sdk.integration;

import cloud.prefab.domain.Prefab;
import com.google.common.collect.ImmutableMap;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import com.reforge.sdk.internal.ConfigLoader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefabContextFactory {

  static final Logger LOG = LoggerFactory.getLogger(PrefabContextFactory.class);

  public static ContextSetReadable from(Map<String, Map<String, Object>> context) {
    if (context == null) {
      return ContextSetReadable.EMPTY;
    }
    ContextSet prefabContextSet = new ContextSet();
    for (Map.Entry<String, Map<String, Object>> stringMapEntry : context.entrySet()) {
      prefabContextSet.addContext(
        Context.fromMap(
          stringMapEntry.getKey(),
          fromLevel2Map(stringMapEntry.getKey(), stringMapEntry.getValue())
        )
      );
    }
    return prefabContextSet;
  }

  private static Map<String, Prefab.ConfigValue> fromLevel2Map(
    String contextType,
    Map<String, Object> values
  ) {
    ImmutableMap.Builder<String, Prefab.ConfigValue> builder = ImmutableMap.builder();

    for (Map.Entry<String, Object> stringObjectEntry : values.entrySet()) {
      String key = stringObjectEntry.getKey();
      Object value = stringObjectEntry.getValue();

      if (value instanceof Map) {
        LOG.info(
          "Context {} has unhandled Map entry under key {} with value {}",
          contextType,
          key,
          value
        );
      }
      builder.put(key, ConfigLoader.configValueFromObj(key, value));
    }
    return builder.buildKeepingLast();
  }
}
