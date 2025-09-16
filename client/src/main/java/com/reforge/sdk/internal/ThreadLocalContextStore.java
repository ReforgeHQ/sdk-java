package com.reforge.sdk.internal;

import com.reforge.sdk.context.ContextStore;
import com.reforge.sdk.context.PrefabContext;
import com.reforge.sdk.context.PrefabContextSet;
import com.reforge.sdk.context.PrefabContextSetReadable;
import java.util.Optional;

public class ThreadLocalContextStore implements ContextStore {

  static final ThreadLocal<PrefabContextSet> PREFAB_CONTEXT_SET_THREAD_LOCAL = ThreadLocal.withInitial(
    PrefabContextSet::new
  );
  public static final ThreadLocalContextStore INSTANCE = new ThreadLocalContextStore();

  private ThreadLocalContextStore() {}

  @Override
  public void addContext(PrefabContext prefabContext) {
    PREFAB_CONTEXT_SET_THREAD_LOCAL.get().addContext(prefabContext);
  }

  @Override
  public Optional<PrefabContextSetReadable> setContext(
    PrefabContextSetReadable prefabContextSetReadable
  ) {
    Optional<PrefabContextSet> previousContext = getStoredContextSet();
    PREFAB_CONTEXT_SET_THREAD_LOCAL.set(
      PrefabContextSet.convert(prefabContextSetReadable)
    );
    return previousContext.map(PrefabContextSetReadable::readOnlyContextSetView);
  }

  @Override
  public Optional<PrefabContextSetReadable> clearContext() {
    Optional<PrefabContextSet> previousContext = getStoredContextSet();
    PREFAB_CONTEXT_SET_THREAD_LOCAL.remove();
    return previousContext.map(PrefabContextSetReadable::readOnlyContextSetView);
  }

  @Override
  public Optional<PrefabContextSetReadable> getContext() {
    return getStoredContextSet().map(PrefabContextSetReadable::readOnlyContextSetView);
  }

  private Optional<PrefabContextSet> getStoredContextSet() {
    return Optional.ofNullable(PREFAB_CONTEXT_SET_THREAD_LOCAL.get());
  }
}
