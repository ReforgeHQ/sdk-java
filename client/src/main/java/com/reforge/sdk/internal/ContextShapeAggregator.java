package com.reforge.sdk.internal;

import cloud.prefab.domain.Prefab;
import com.google.common.annotations.VisibleForTesting;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Capture context "shape" and
 */
public class ContextShapeAggregator {

  private static final Logger LOG = LoggerFactory.getLogger(ContextShapeAggregator.class);

  private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> shapes;

  private final AtomicBoolean dirtyFlag = new AtomicBoolean(true);

  ContextShapeAggregator() {
    this.shapes = new ConcurrentHashMap<>();
  }

  void reportContextUsage(ContextSetReadable contextSetReadable) {
    Prefab.ContextShapes currentShapes = extractShapes(contextSetReadable);
    for (Prefab.ContextShape contextShape : currentShapes.getShapesList()) {
      ConcurrentHashMap<String, Integer> contextMap = shapes.computeIfAbsent(
        contextShape.getName(),
        key -> new ConcurrentHashMap<>()
      );
      contextShape
        .getFieldTypesMap()
        .forEach((key, value) -> {
          Integer oldValue = contextMap.put(key, value);
          if (!Objects.equals(oldValue, value)) {
            boolean dirtyFlagRaised = dirtyFlag.compareAndSet(false, true);
            if (dirtyFlagRaised && LOG.isTraceEnabled()) {
              LOG.trace(
                "dirty flag raised by context name: {} and property {}",
                contextShape.getName(),
                key
              );
            }
          }
        });
    }
  }

  Optional<Prefab.ContextShapes> getShapesIfNewInfo() {
    if (dirtyFlag.get()) {
      return Optional.of(getShapes());
    }
    return Optional.empty();
  }

  Prefab.ContextShapes getShapes() {
    return buildProtoShapesFromShapeState();
  }

  @VisibleForTesting
  Prefab.ContextShapes buildProtoShapesFromShapeState() {
    Prefab.ContextShapes.Builder shapesBuilder = Prefab.ContextShapes.newBuilder();
    shapes.forEach((contextName, contextMap) -> {
      Prefab.ContextShape.Builder shapeBuilder = Prefab.ContextShape
        .newBuilder()
        .setName(contextName);
      shapeBuilder.putAllFieldTypes(contextMap);
      shapesBuilder.addShapes(shapeBuilder);
    });

    return shapesBuilder.build();
  }

  private Prefab.ContextShapes extractShapes(
    ContextSetReadable contextSetReadable
  ) {
    Prefab.ContextShapes.Builder shapesBuilder = Prefab.ContextShapes.newBuilder();
    StreamSupport
      .stream(contextSetReadable.getContexts().spliterator(), false)
      .map(Context::getShape)
      .forEach(shapesBuilder::addShapes);
    return shapesBuilder.build();
  }
}
