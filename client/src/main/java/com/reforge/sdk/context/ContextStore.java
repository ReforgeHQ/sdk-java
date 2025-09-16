package com.reforge.sdk.context;

import java.util.Optional;

public interface ContextStore {
  /**
   * Adds a context to the set of contexts for the current context-session scope
   * If there's already a context with the same type stored in the scope,
   * it is overwritten
   * @param context a context -
   */
  void addContext(Context context);

  /**
   * Overwrites any existing context with the provided context
   * @param contextSetReadable
   * @return existing context, if present
   */
  Optional<ContextSetReadable> setContext(
    ContextSetReadable contextSetReadable
  );

  /**
   * Removes all prefab contexts the current context session scope
   * (By default, this is stored in a ThreadLocal)
   * @return existing context, if present
   */
  Optional<ContextSetReadable> clearContext();

  /**
   *
   * @return unmodifiable PrefabContextSetReadable view
   */
  Optional<ContextSetReadable> getContext();

  /**
   *
   * @return true or false to indicate if the underlying platform feature for a given ContextStore is available
   */

  default boolean isAvailable() {
    return true;
  }
}
