package com.reforge.sdk.internal;

import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import com.reforge.sdk.context.ContextStore;
import java.util.Optional;

public class ThreadLocalContextStore implements ContextStore {

  static final ThreadLocal<ContextSet> PREFAB_CONTEXT_SET_THREAD_LOCAL = ThreadLocal.withInitial(
    ContextSet::new
  );
  public static final ThreadLocalContextStore INSTANCE = new ThreadLocalContextStore();

  private ThreadLocalContextStore() {}

  @Override
  public void addContext(Context context) {
    PREFAB_CONTEXT_SET_THREAD_LOCAL.get().addContext(context);
  }

  @Override
  public Optional<ContextSetReadable> setContext(ContextSetReadable contextSetReadable) {
    Optional<ContextSet> previousContext = getStoredContextSet();
    PREFAB_CONTEXT_SET_THREAD_LOCAL.set(ContextSet.convert(contextSetReadable));
    return previousContext.map(ContextSetReadable::readOnlyContextSetView);
  }

  @Override
  public Optional<ContextSetReadable> clearContext() {
    Optional<ContextSet> previousContext = getStoredContextSet();
    PREFAB_CONTEXT_SET_THREAD_LOCAL.remove();
    return previousContext.map(ContextSetReadable::readOnlyContextSetView);
  }

  @Override
  public Optional<ContextSetReadable> getContext() {
    return getStoredContextSet().map(ContextSetReadable::readOnlyContextSetView);
  }

  private Optional<ContextSet> getStoredContextSet() {
    return Optional.ofNullable(PREFAB_CONTEXT_SET_THREAD_LOCAL.get());
  }
}
