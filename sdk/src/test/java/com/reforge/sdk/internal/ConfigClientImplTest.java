package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.Options;
import com.reforge.sdk.Sdk;
import com.reforge.sdk.SdkInitializationTimeoutException;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextHelper;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigClientImplTest {

  @Test
  void localModeUnlocks() {
    final Sdk baseClient = new Sdk(
      new Options().setDatasource(Options.Datasources.LOCAL_ONLY)
    );
    ConfigClient configClient = new ConfigClientImpl(baseClient);

    final Optional<Prefab.ConfigValue> key = configClient.get("key");
    assertThat(key).isNotPresent();
  }

  @Test
  void initializationTimeout() {
    final Sdk baseClient = new Sdk(
      new Options()
        .setApikey("0-P1-E1-SDK-1234-123-23")
        .setInitializationTimeoutSec(1)
        .setOnInitializationFailure(Options.OnInitializationFailure.RAISE)
    );

    ConfigClient configClient = new ConfigClientImpl(baseClient);
    assertThrows(SdkInitializationTimeoutException.class, () -> configClient.get("key"));
  }

  @Test
  void initializationUnlock() {
    final Sdk baseClient = new Sdk(
      new Options()
        .setApikey("0-P1-E1-SDK-1234-123-23")
        .setInitializationTimeoutSec(1)
        .setOnInitializationFailure(Options.OnInitializationFailure.UNLOCK)
    );

    ConfigClient configClient = new ConfigClientImpl(baseClient);
    assertThat(configClient.get("key")).isNotPresent();
  }

  @Test
  void localDataFileMode() {
    final Sdk baseClient = new Sdk(
      new Options()
        .setLocalDatafile("src/test/resources/prefab.Development.5.config.json")
    );
    ConfigClient configClient = new ConfigClientImpl(baseClient);
    final Optional<Prefab.ConfigValue> key = configClient.get("cool.bool.enabled");
    assertThat(key).isPresent();
  }

  @Nested
  class ContextTests {

    @Captor
    ArgumentCaptor<LookupContext> lookupContextArgumentCaptor;

    @Mock
    UpdatingConfigResolver updatingConfigResolver;

    @Mock
    Sdk sdk;

    ConfigClientImpl configClient;
    private ContextHelper contextHelper;

    @BeforeEach
    void beforeEach() {
      Options options = new Options().setDatasource(Options.Datasources.LOCAL_ONLY);
      when(sdk.getOptions()).thenReturn(options);
      when(updatingConfigResolver.update())
        .thenReturn(new UpdatingConfigResolver.ChangeLists(Collections.emptyList()));

      this.configClient = new ConfigClientImpl(sdk, updatingConfigResolver);
      this.contextHelper = new ContextHelper(configClient);
    }

    @Test
    void requestWithNoPassedContextHasAnEmptyLookupContext() {
      configClient.get("foobar");
      verify(updatingConfigResolver)
        .getMatch("foobar", new LookupContext(ContextSetReadable.EMPTY));
    }

    @Test
    void requestWithPassedContextHasSameInLookupContext() {
      Context context = Context
        .newBuilder("user")
        .put("name", "james")
        .put("isHuman", true)
        .build();

      configClient.get("foobar", context);
      verify(updatingConfigResolver).getMatch("foobar", new LookupContext(context));
    }

    @Test
    void requestWithGlobalContextAndNoPassedContextHasExpectedLookup() {
      Context context = Context
        .newBuilder("user")
        .put("name", "james")
        .put("isHuman", true)
        .build();

      try (
        ContextHelper.PrefabContextScope ignored = contextHelper.performWorkWithAutoClosingContext(
          context
        )
      ) {
        configClient.get("foobar");
        verify(updatingConfigResolver)
          .getMatch(eq("foobar"), lookupContextArgumentCaptor.capture());
      }

      LookupContext lookupContext = lookupContextArgumentCaptor.getValue();
      LookupContext expected = new LookupContext(ContextSet.from(context));

      assertThat(lookupContext).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void requestWithGlobalContextAndConflictingPassedContextHasExpectedLookup() {
      ContextSet globalUserContext = ContextSet.from(
        Context
          .newBuilder("user")
          .put("name", "james")
          .put("isHuman", true)
          .put("somethingCount", 11)
          .build(),
        Context.newBuilder("computer").put("greeting", "hello computer").build()
      );

      ContextSet localUserContext = ContextSet.from(
        Context.newBuilder("user").put("name", "roboto").put("isHuman", false).build(),
        Context.newBuilder("transaction").put("type", "credit").build()
      );

      try (
        ContextHelper.PrefabContextScope ignored = contextHelper.performWorkWithAutoClosingContext(
          globalUserContext
        )
      ) {
        configClient.get("foobar", localUserContext);
        verify(updatingConfigResolver)
          .getMatch(eq("foobar"), lookupContextArgumentCaptor.capture());
      }
      LookupContext lookupContext = lookupContextArgumentCaptor.getValue();

      LookupContext expected = new LookupContext(
        ContextSet.from(
          Context.newBuilder("user").put("name", "roboto").put("isHuman", false).build(),
          Context.newBuilder("computer").put("greeting", "hello computer").build(),
          Context.newBuilder("transaction").put("type", "credit").build()
        )
      );

      assertThat(lookupContext).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
