package com.reforge.sdk.extensions.micronaut;

import static org.assertj.core.api.Assertions.assertThat;

import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.simple.SimpleHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServerRequestContextStoreTest {

  ServerRequestContextStore reforgeSdkContextStore = new ServerRequestContextStore();
  Context userContext = Context.newBuilder("user").put("country", "us").build();
  Context serverContext = Context.newBuilder("server").put("az", "2").build();

  ContextSet userAndServerContextSet = ContextSet.from(userContext, serverContext);

  @Nested
  class MissingHttpRequest {

    @BeforeEach
    void beforeEach() {
      ServerRequestContext.set(null);
    }

    @Test
    void getContextReturnsEmpty() {
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }

    @Test
    void setContextQuietlyDoesNothing() {
      assertThat(reforgeSdkContextStore.setContext(userAndServerContextSet)).isEmpty();
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }

    @Test
    void addContextQuietlyDoesNothing() {
      reforgeSdkContextStore.addContext(userContext);
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }

    @Test
    void clearContextQuietlyDoesNothing() {
      assertThat(reforgeSdkContextStore.clearContext()).isEmpty();
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }
  }

  @Nested
  class WithHttpRequest {

    @BeforeEach
    void beforeEach() {
      ServerRequestContext.set(
        new SimpleHttpRequest<>(HttpMethod.POST, "http://localhost/", "The body")
      );
    }

    @Test
    void getContextReturnsEmptyWhenNoContextSet() {
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }

    @Test
    void setContextReturnsEmptyWhenNoContextSet() {
      assertThat(reforgeSdkContextStore.setContext(userAndServerContextSet)).isEmpty();
    }

    @Test
    void addContextWhenEmptyUpdatesTheContext() {
      reforgeSdkContextStore.addContext(userContext);
      assertThat(reforgeSdkContextStore.getContext())
        .isPresent()
        .get()
        .usingRecursiveComparison()
        .isEqualTo(ContextSet.convert(userContext));
    }

    @Test
    void clearReturnsEmpty() {
      assertThat(reforgeSdkContextStore.clearContext()).isEmpty();
      assertThat(reforgeSdkContextStore.getContext()).isEmpty();
    }

    @Nested
    class WithPreExistingContext {

      Context newUserContext = Context.newBuilder("user").put("country", "UK").build();
      ContextSet newUserAndServerContextSet = ContextSet.from(
        newUserContext,
        serverContext
      );

      @BeforeEach
      void beforeEach() {
        reforgeSdkContextStore.setContext(userAndServerContextSet);
      }

      @Test
      void getReturnsExpectedSet() {
        assertThat(reforgeSdkContextStore.getContext())
          .isPresent()
          .get()
          .usingRecursiveComparison()
          .isEqualTo(userAndServerContextSet);
      }

      @Test
      void clearWorksAsExpected() {
        assertThat(reforgeSdkContextStore.clearContext())
          .isPresent()
          .get()
          .usingRecursiveComparison()
          .isEqualTo(userAndServerContextSet);
        assertThat(reforgeSdkContextStore.getContext()).isEmpty();
      }

      @Test
      void setWorksAsExpected() {
        assertThat(reforgeSdkContextStore.setContext(userContext))
          .isPresent()
          .get()
          .usingRecursiveComparison()
          .isEqualTo(userAndServerContextSet);
        assertThat(reforgeSdkContextStore.getContext())
          .isPresent()
          .get()
          .usingRecursiveComparison()
          .isEqualTo(userContext);
      }

      @Test
      void addWorksAsExpected() {
        reforgeSdkContextStore.addContext(newUserContext);
        assertThat(reforgeSdkContextStore.getContext())
          .isPresent()
          .get()
          .usingRecursiveComparison()
          .isEqualTo(newUserAndServerContextSet);
      }
    }
  }
}
