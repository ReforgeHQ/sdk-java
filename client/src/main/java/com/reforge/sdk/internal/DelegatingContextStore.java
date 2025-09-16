package com.reforge.sdk.internal;

import com.reforge.sdk.context.ContextStore;
import com.reforge.sdk.context.PrefabContext;
import com.reforge.sdk.context.PrefabContextSetReadable;
import java.util.Optional;

public abstract class DelegatingContextStore implements ContextStore {

  abstract ContextStore getContextStore();

  @Override
  public void addContext(PrefabContext prefabContext) {
    getContextStore().addContext(prefabContext);
  }

  @Override
  public Optional<PrefabContextSetReadable> clearContext() {
    return getContextStore().clearContext();
  }

  @Override
  public Optional<PrefabContextSetReadable> setContext(
    PrefabContextSetReadable prefabContextSetReadable
  ) {
    return Optional.empty();
  }

  @Override
  public Optional<PrefabContextSetReadable> getContext() {
    return getContextStore().getContext();
  }
}
