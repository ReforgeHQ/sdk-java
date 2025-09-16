package com.reforge.sdk.internal;

import com.google.common.base.Predicates;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ContextMerger {

  public static ContextSetReadable merge(
    @Nullable ContextSetReadable globalContext,
    @Nullable ContextSetReadable apiDefaultContext,
    @Nullable ContextSetReadable contextStoreContext,
    @Nullable ContextSetReadable passedContext
  ) {
    // use naive strategy for now,
    ContextSet prefabContextSet = new ContextSet();

    Stream
      .of(globalContext, apiDefaultContext, contextStoreContext, passedContext)
      .filter(Predicates.notNull())
      .filter(Predicate.not(ContextSetReadable::isEmpty))
      .forEach(prefabContextSetReadable -> {
        for (Context context : prefabContextSetReadable.getContexts()) {
          prefabContextSet.addContext(context);
        }
      });

    return prefabContextSet;
  }
}
