package com.reforge.sdk.context;

import cloud.prefab.domain.Prefab;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public class ContextSet implements ContextSetReadable {

  private final ConcurrentSkipListMap<String, Context> contextByNameMap = new ConcurrentSkipListMap<>();

  public ContextSet addContext(Context context) {
    if (context != null) {
      contextByNameMap.put(context.getName().toLowerCase(), context);
    }
    return this;
  }

  public boolean isEmpty() {
    return contextByNameMap.isEmpty();
  }

  @Override
  public Optional<Context> getByName(String contextType) {
    return Optional.ofNullable(contextByNameMap.get(contextType.toLowerCase()));
  }

  @Override
  public Iterable<Context> getContexts() {
    return ImmutableList.copyOf(contextByNameMap.values());
  }

  public static ContextSet from(Context... contexts) {
    ContextSet set = new ContextSet();
    for (Context context : contexts) {
      set.addContext(context);
    }
    return set;
  }

  /**
   * Converts the given `PrefabContextSetReadable` instance into a PrefabContextSet
   * If the argument is already a PrefabContextSet return it, othewise create a new PrefabContextSet and add the contents
   * of the PrefabContextSetReadable to it, then return the new set
   * @param contextSetReadable instance to convert
   * @return a PrefabContextSet built as discussed above
   */
  public static ContextSet convert(
    ContextSetReadable contextSetReadable
  ) {
    if (contextSetReadable instanceof ContextSet) {
      return (ContextSet) contextSetReadable;
    }
    ContextSet prefabContextSet = new ContextSet();
    for (Context context : contextSetReadable.getContexts()) {
      prefabContextSet.addContext(context);
    }
    return prefabContextSet;
  }

  public Prefab.ContextSet toProto() {
    Prefab.ContextSet.Builder bldr = Prefab.ContextSet.newBuilder();
    getContexts()
      .forEach(prefabContext -> bldr.addContexts(prefabContext.toProtoContext()));
    return bldr.build();
  }

  public static ContextSetReadable from(Prefab.ContextSet protoContextSet) {
    if (protoContextSet.getContextsList().isEmpty()) {
      return ContextSet.EMPTY;
    }
    ContextSet prefabContextSet = new ContextSet();
    for (Prefab.Context contextProto : protoContextSet.getContextsList()) {
      prefabContextSet.addContext(Context.fromProto(contextProto));
    }
    return prefabContextSet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContextSet that = (ContextSet) o;
    return Objects.equals(contextByNameMap, that.contextByNameMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contextByNameMap);
  }

  @Override
  public String toString() {
    return com.google.common.base.MoreObjects
      .toStringHelper(this)
      .add("contextByNameMap", contextByNameMap)
      .toString();
  }
}
