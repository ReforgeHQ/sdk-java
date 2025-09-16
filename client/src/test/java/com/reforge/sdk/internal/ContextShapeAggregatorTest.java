package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContextShapeAggregatorTest {

  @Captor
  ArgumentCaptor<Prefab.ContextShapes> shapesArgumentCaptor;

  private ContextShapeAggregator aggregator;

  @BeforeEach
  void beforeEach() {
    aggregator = new ContextShapeAggregator();
  }

  // make sure the data comes out as expected
  @Test
  void sendsCorrectData() {
    aggregator.reportContextUsage(
      Context
        .newBuilder("user")
        .put("tier", "gold")
        .put("age", 44)
        .put("alive", true)
        .build()
    );

    aggregator.reportContextUsage(
      Context
        .newBuilder("user")
        .put("tier", "silver")
        .put("age", 100)
        .put("alive", true)
        .put("foo", "bar")
        .build()
    );

    aggregator.reportContextUsage(
      Context.newBuilder("").put("something", "else").build()
    );

    Prefab.ContextShapes reportedShape = aggregator.getShapes();

    assertThat(reportedShape.getShapesList())
      .containsExactlyInAnyOrder(
        Prefab.ContextShape
          .newBuilder()
          .setName("user")
          .putFieldTypes("age", Prefab.ConfigValue.TypeCase.INT.getNumber())
          .putFieldTypes("tier", Prefab.ConfigValue.TypeCase.STRING.getNumber())
          .putFieldTypes("alive", Prefab.ConfigValue.TypeCase.BOOL.getNumber())
          .putFieldTypes("foo", Prefab.ConfigValue.TypeCase.STRING.getNumber())
          .build(),
        Prefab.ContextShape
          .newBuilder()
          .setName("")
          .putFieldTypes("something", Prefab.ConfigValue.TypeCase.STRING.getNumber())
          .build()
      );
  }
}
