package com.reforge.sdk.internal;

import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSetReadable;
import com.reforge.sdk.context.ContextStore;
import java.util.Optional;

public abstract class DelegatingContextStore implements ContextStore {

  abstract ContextStore getContextStore();

  @Override
  public void addContext(Context context) {
    getContextStore().addContext(context);
  }

  @Override
  public Optional<ContextSetReadable> clearContext() {
    return getContextStore().clearContext();
  }

  @Override
  public Optional<ContextSetReadable> setContext(ContextSetReadable contextSetReadable) {
    return Optional.empty();
  }

  @Override
  public Optional<ContextSetReadable> getContext() {
    return getContextStore().getContext();
  }
}
