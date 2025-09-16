package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import org.junit.jupiter.api.Test;

class ContextMergerTest {

  private static final ContextSet GLOBAL = ContextSet.from(
    Context.newBuilder("a").put("ga-foo", "bar").put("ga-abc", 123).build(),
    Context.newBuilder("b").put("gb-foo", "bar").put("gb-abc", 123).build(),
    Context.newBuilder("global").put("sunny", "day").put("solar", 123).build()
  );

  private static final ContextSet API = ContextSet.from(
    Context.newBuilder("a").put("api-a-foo", "bar").put("api-a-abc", 123).build(),
    Context.newBuilder("b").put("api-a-foo", "bar").put("api-a-abc", 123).build(),
    Context.newBuilder("api").put("cloudy", "day").put("solar", 234).build()
  );

  private static final ContextSet CURRENT = ContextSet.from(
    Context.newBuilder("a").put("current-a-foo", "bar").put("current-a-abc", 123).build(),
    Context.newBuilder("b").put("current-a-foo", "bar").put("current-a-abc", 123).build(),
    Context.newBuilder("current").put("rainy", "day").put("solar", 456).build()
  );

  private static final ContextSet PASSED = ContextSet.from(
    Context.newBuilder("a").put("passed-a-foo", "bar").put("passed-a-abc", 123).build(),
    Context.newBuilder("b").put("passed-a-foo", "bar").put("passed-a-abc", 123).build(),
    Context.newBuilder("passed").put("foggy", "day").put("solar", 345).build()
  );

  @Test
  void itReturnsEmptyContextForAllNullArguments() {
    assertThat(ContextMerger.merge(null, null, null, null).isEmpty()).isTrue();
  }

  @Test
  void itReturnsEmptyContextForAllEmptyArguments() {
    assertThat(
      ContextMerger
        .merge(
          ContextSetReadable.EMPTY,
          ContextSetReadable.EMPTY,
          ContextSetReadable.EMPTY,
          ContextSetReadable.EMPTY
        )
        .isEmpty()
    )
      .isTrue();
  }

  @Test
  void itMergesGlobalWithApiInCorrectOrderWithoutPassedOrCurrentContext() {
    ContextSetReadable merged = ContextMerger.merge(GLOBAL, API, CURRENT, PASSED);
    assertThat(merged)
      .isEqualTo(
        PASSED
          .addContext(GLOBAL.getByName("global").get())
          .addContext(API.getByName("api").get())
          .addContext(CURRENT.getByName("current").get())
      );
  }

  @Test
  void itMergesAllContextsCorrectly() {
    ContextSetReadable merged = ContextMerger.merge(
      GLOBAL,
      API,
      ContextSetReadable.EMPTY,
      ContextSetReadable.EMPTY
    );

    assertThat(merged).isEqualTo(API.addContext(GLOBAL.getByName("global").get()));
  }
}
