package com.reforge.client.micronaut;

import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import com.reforge.sdk.context.ContextStore;
import io.micronaut.http.context.ServerRequestContext;
import java.util.Optional;

/**
 * This supersedes the standard ThreadLocalContext store for micronaut because
 * micronaut is an event-based server, so a request can be handled by many threads.
 * Instead of using a ThreadLocal, we stash the context into the attributes of the current
 * HttpRequest via ServerRequestContext.currentRequest()
 */
public class ServerRequestContextStore implements ContextStore {

  public static final String ATTRIBUTE_NAME = "prefab-contexts";

  @Override
  public void addContext(Context context) {
    getPrefabContextSet()
      .ifPresentOrElse(
        prefabContextSet -> prefabContextSet.addContext(context),
        () -> setContext(context)
      );
  }

  @Override
  public Optional<ContextSetReadable> setContext(ContextSetReadable contextSetReadable) {
    return ServerRequestContext
      .currentRequest()
      .map(req -> {
        Optional<ContextSetReadable> currentContext = getContext();
        req.setAttribute(ATTRIBUTE_NAME, ContextSet.convert(contextSetReadable));
        return currentContext;
      })
      .orElse(Optional.empty());
  }

  @Override
  public Optional<ContextSetReadable> clearContext() {
    Optional<ContextSetReadable> currentContext = getContext();
    ServerRequestContext
      .currentRequest()
      .ifPresent(objectHttpRequest -> objectHttpRequest.setAttribute(ATTRIBUTE_NAME, null)
      );
    return currentContext;
  }

  @Override
  public Optional<ContextSetReadable> getContext() {
    return getPrefabContextSet().map(ContextSetReadable::readOnlyContextSetView);
  }

  private Optional<ContextSet> getPrefabContextSet() {
    return ServerRequestContext
      .currentRequest()
      .flatMap(objectHttpRequest ->
        objectHttpRequest.getAttribute(ATTRIBUTE_NAME, ContextSet.class)
      );
  }

  @Override
  public boolean isAvailable() {
    return ServerRequestContext.currentRequest().isPresent();
  }
}
