package com.reforge.sdk.context;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.config.ConfigValueUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Context implements ContextSetReadable {

  private final String name;

  private final Map<String, Prefab.ConfigValue> properties;

  private Context(String name, Map<String, Prefab.ConfigValue> properties) {
    this.name = name;
    this.properties = Map.copyOf(properties);
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean isEmpty() {
    return properties.isEmpty();
  }

  public Map<String, Prefab.ConfigValue> getProperties() {
    return properties;
  }

  public Map<String, Prefab.ConfigValue> getNameQualifiedProperties() {
    if (name.isBlank()) {
      return getProperties();
    }
    String prefix = getName().toLowerCase() + ".";

    return properties
      .entrySet()
      .stream()
      .map(entry -> Map.entry(prefix + entry.getKey(), entry.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  // implementation of PrefabContextSetReadable so one context is interchangable with many
  @Override
  public Optional<Context> getByName(String contextType) {
    if (contextType.equals(this.name)) {
      return Optional.of(this);
    }
    return Optional.empty();
  }

  @Override
  public Iterable<Context> getContexts() {
    return Collections.singleton(this);
  }

  public Prefab.Context toProtoContext() {
    return Prefab.Context
      .newBuilder()
      .setType(getName())
      .putAllValues(getProperties())
      .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Context that = (Context) o;
    return (
      Objects.equals(name, that.name) && Objects.equals(properties, that.properties)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, properties);
  }

  @Override
  public String toString() {
    return com.google.common.base.MoreObjects
      .toStringHelper(this)
      .add("name", name)
      .add("properties", properties)
      .toString();
  }

  public Prefab.ContextShape getShape() {
    Prefab.ContextShape.Builder shapeBuilder = Prefab.ContextShape
      .newBuilder()
      .setName(getName());
    properties.forEach((key, value) ->
      shapeBuilder.putFieldTypes(key, value.getTypeCase().getNumber())
    );
    return shapeBuilder.build();
  }

  public static Context unnamedFromMap(Map<String, Prefab.ConfigValue> configValueMap) {
    return new Context("", configValueMap);
  }

  public static Context fromMap(
    String name,
    Map<String, Prefab.ConfigValue> configValueMap
  ) {
    return new Context(name, configValueMap);
  }

  public static Context fromProto(Prefab.Context protoContext) {
    return new Context(protoContext.getType(), protoContext.getValuesMap());
  }

  public static class Builder {

    private final String contextType;
    private final Map<String, Prefab.ConfigValue> properties = new HashMap<>();

    private Builder(String contextType) {
      this.contextType = contextType;
    }

    public Builder put(String propertyName, String value) {
      return put(propertyName, ConfigValueUtils.from(value));
    }

    public Builder put(String propertyName, boolean value) {
      return put(propertyName, ConfigValueUtils.from(value));
    }

    public Builder put(String propertyName, long value) {
      return put(propertyName, ConfigValueUtils.from(value));
    }

    public Builder put(String propertyName, double value) {
      return put(propertyName, ConfigValueUtils.from(value));
    }

    public Builder put(String propertyName, List<String> value) {
      return put(propertyName, ConfigValueUtils.from(value));
    }

    public Builder put(String propertyName, Prefab.ConfigValue configValue) {
      properties.put(propertyName, configValue);
      return this;
    }

    public Context build() {
      return new Context(contextType, properties);
    }
  }

  public static Builder newBuilder(String contextType) {
    return new Builder(contextType);
  }
}
