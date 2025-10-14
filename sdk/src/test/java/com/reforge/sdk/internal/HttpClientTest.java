package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.prefab.domain.Prefab;
import com.google.common.cache.Cache;
import com.reforge.sdk.Options;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpClientTest {

  @Mock
  java.net.http.HttpClient mockHttpClient;

  Options options = new Options()
    .setApiHosts(List.of("http://a.example.com", "http://b.example.com"))
    .setStreamHosts(List.of("http://stream.example.com"))
    .setSdkKey("not-a-real-key")
    .setTelemetryHost("http://telemetry.example.com");

  HttpClient prefabHttpClient;

  @BeforeEach
  void setup() {
    prefabHttpClient = new HttpClient(mockHttpClient, options);
    // Clear the internal cache before each test.
    prefabHttpClient.clearCache();
  }

  @Test
  void testFailoverForConfigFetch() throws Exception {
    // Use byte[]–based mocks since requestConfigsFromURI uses BodyHandlers.ofByteArray().
    Prefab.Configs dummyConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L)
      )
      .build();
    byte[] dummyBytes = dummyConfigs.toByteArray();

    HttpResponse<byte[]> failureResponse = mock(HttpResponse.class);
    when(failureResponse.statusCode()).thenReturn(500);

    HttpResponse<byte[]> successResponse = mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);
    when(successResponse.body()).thenReturn(dummyBytes);
    // Provide minimal headers for the success branch.
    HttpHeaders successHeaders = HttpHeaders.of(Map.of(), (k, v) -> true);
    when(successResponse.headers()).thenReturn(successHeaders);

    // Set up stubbing: 2 failures then a success.
    CompletableFuture<HttpResponse<byte[]>> failureFuture = CompletableFuture.completedFuture(
      failureResponse
    );
    CompletableFuture<HttpResponse<byte[]>> successFuture = CompletableFuture.completedFuture(
      successResponse
    );
    // We use a simple stubbing (without capturing) and then later verify the number of invocations.
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(failureFuture, failureFuture, successFuture);

    // Invoke requestConfigs(0L). This goes through our failover logic.
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> result = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> response = result.get();

    // Verify that after 2 failures we eventually succeed.
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body().get()).isEqualTo(dummyConfigs);
    verify(mockHttpClient, times(3)).sendAsync(any(), any());
    // (Optionally, if you wish to capture the URIs, you can do so via a custom Answer.)
  }

  @Test
  void testFailoverForSSEConnection() throws Exception {
    // This test remains essentially unchanged.
    HttpResponse<Supplier<Prefab.Configs>> failureResponse = mock(HttpResponse.class);
    when(failureResponse.statusCode()).thenReturn(500);

    HttpResponse<Supplier<Prefab.Configs>> successResponse = mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);

    Flow.Subscriber<String> lineSubscriber = mock(Flow.Subscriber.class);

    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> failureFuture = CompletableFuture.completedFuture(
      failureResponse
    );
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> successFuture = CompletableFuture.completedFuture(
      successResponse
    );

    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(failureFuture, failureFuture, successFuture);

    CompletableFuture<HttpResponse<Void>> responseFuture = prefabHttpClient.createSSEConfigConnection(
      0L,
      lineSubscriber
    );
    HttpResponse<Void> response = responseFuture.get();

    assertThat(response.statusCode()).isEqualTo(200);
    verify(mockHttpClient, times(3)).sendAsync(any(), any());
  }

  @Test
  void testBasicCaching() throws Exception {
    Prefab.Configs dummyConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L)
      )
      .build();
    byte[] dummyBytes = dummyConfigs.toByteArray();

    HttpResponse<byte[]> httpResponse200 = mock(HttpResponse.class);
    when(httpResponse200.statusCode()).thenReturn(200);
    when(httpResponse200.body()).thenReturn(dummyBytes);
    HttpHeaders headers = HttpHeaders.of(
      Map.of("Cache-Control", List.of("max-age=60"), "ETag", List.of("abc")),
      (k, v) -> true
    );
    when(httpResponse200.headers()).thenReturn(headers);

    CompletableFuture<HttpResponse<byte[]>> future200 = CompletableFuture.completedFuture(
      httpResponse200
    );
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(future200);

    // First call should go over the network (MISS) and then cache the response.
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> resultFuture1 = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> result1 = resultFuture1.get();
    assertThat(result1.statusCode()).isEqualTo(200);
    assertThat(result1.headers().firstValue("X-Cache")).contains("MISS");
    assertThat(result1.body().get()).isEqualTo(dummyConfigs);
    verify(mockHttpClient, times(1)).sendAsync(any(), any());

    // Second call should return the cached result (HIT) without a network call.
    reset(mockHttpClient);
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> resultFuture2 = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> result2 = resultFuture2.get();
    assertThat(result2.headers().firstValue("X-Cache")).contains("HIT");
    assertThat(result2.body().get()).isEqualTo(dummyConfigs);
    verify(mockHttpClient, times(0)).sendAsync(any(), any());
  }

  @Test
  void testConditionalGet304() throws Exception {
    // In order to trigger a conditional GET, we insert a cached entry that is expired.
    Prefab.Configs dummyConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L)
      )
      .build();
    byte[] dummyBytes = dummyConfigs.toByteArray();
    // Use a time far enough in the past to ensure expiration.
    long past = System.currentTimeMillis() - 10_000;

    URI uri = URI.create("http://a.example.com/api/v2/configs/0");
    Field cacheField = HttpClient.class.getDeclaredField("configCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Cache<URI, HttpClient.CacheEntry> cache = (Cache<URI, HttpClient.CacheEntry>) cacheField.get(
      prefabHttpClient
    );
    HttpClient.CacheEntry expiredEntry = new HttpClient.CacheEntry(
      dummyBytes,
      "abc",
      past
    );
    cache.put(uri, expiredEntry);

    // Mark the stubbing for sendAsync as lenient so that if it's not invoked, we don't fail.
    HttpResponse<byte[]> httpResponse304 = mock(HttpResponse.class);
    when(httpResponse304.statusCode()).thenReturn(304);

    CompletableFuture<HttpResponse<byte[]>> future304 = CompletableFuture.completedFuture(
      httpResponse304
    );
    lenient()
      .when(
        mockHttpClient.sendAsync(
          any(HttpRequest.class),
          any(HttpResponse.BodyHandler.class)
        )
      )
      .thenReturn(future304);

    // This call should notice the cached entry (even if expired) and, upon receiving 304,
    // return the cached value.
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> resultFuture = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> result = resultFuture.get();
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(result.headers().firstValue("X-Cache")).contains("HIT");
    assertThat(result.body().get()).isEqualTo(dummyConfigs);
    // Verify that sendAsync was invoked once.
    verify(mockHttpClient, times(1)).sendAsync(any(), any());
  }

  @Test
  void testClearCache() throws Exception {
    Prefab.Configs dummyConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L)
      )
      .build();
    byte[] dummyBytes = dummyConfigs.toByteArray();

    HttpResponse<byte[]> httpResponse200 = mock(HttpResponse.class);
    when(httpResponse200.statusCode()).thenReturn(200);
    when(httpResponse200.body()).thenReturn(dummyBytes);
    HttpHeaders headers = HttpHeaders.of(
      Map.of("Cache-Control", List.of("max-age=60"), "ETag", List.of("abc")),
      (k, v) -> true
    );
    when(httpResponse200.headers()).thenReturn(headers);

    CompletableFuture<HttpResponse<byte[]>> future200 = CompletableFuture.completedFuture(
      httpResponse200
    );
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(future200);

    // First call caches the response.
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> resultFuture1 = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> result1 = resultFuture1.get();
    assertThat(result1.headers().firstValue("X-Cache")).contains("MISS");
    assertThat(prefabHttpClient.getCacheSize()).isEqualTo(1);

    // Clear the cache.
    prefabHttpClient.clearCache();
    assertThat(prefabHttpClient.getCacheSize()).isEqualTo(0);

    // Subsequent call should trigger a network call.
    reset(mockHttpClient);
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(future200);
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> resultFuture2 = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> result2 = resultFuture2.get();
    assertThat(result2.headers().firstValue("X-Cache")).contains("MISS");
    verify(mockHttpClient, times(1)).sendAsync(any(), any());
  }

  @Test
  void testNoCacheResponseAlwaysRevalidates() throws Exception {
    // Create a valid Prefab.Configs instance and its serialized form.
    Prefab.Configs dummyConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L)
      )
      .build();
    byte[] dummyBytes = dummyConfigs.toByteArray();

    // Simulate a 200 response with Cache-Control: no-cache and an ETag.
    HttpResponse<byte[]> response200 = mock(HttpResponse.class);
    when(response200.statusCode()).thenReturn(200);
    when(response200.body()).thenReturn(dummyBytes);
    HttpHeaders headersNoCache = HttpHeaders.of(
      Map.of("Cache-Control", List.of("no-cache"), "ETag", List.of("etag-no-cache")),
      (k, v) -> true
    );
    when(response200.headers()).thenReturn(headersNoCache);

    // First call: should update the cache but mark it as immediately expired.
    CompletableFuture<HttpResponse<byte[]>> future200 = CompletableFuture.completedFuture(
      response200
    );
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(future200);

    // Invoke the request (using offset 0L, yielding a URL like "http://a.example.com/api/v2/configs/0").
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> firstCall = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> resp1 = firstCall.get();

    // The first response should be a network call (MISS).
    assertThat(resp1.statusCode()).isEqualTo(200);
    assertThat(resp1.headers().firstValue("X-Cache")).contains("MISS");

    // Retrieve the cached entry.
    URI uri = URI.create("http://a.example.com/api/v2/configs/0");
    Field cacheField = HttpClient.class.getDeclaredField("configCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Cache<URI, HttpClient.CacheEntry> cache = (Cache<URI, HttpClient.CacheEntry>) cacheField.get(
      prefabHttpClient
    );
    HttpClient.CacheEntry cachedEntry = cache.getIfPresent(uri);
    assertThat(cachedEntry).isNotNull();
    // Due to "no-cache", the cached entry should be immediately expired.
    assertThat(cachedEntry.expiresAt).isLessThanOrEqualTo(System.currentTimeMillis());

    // Now simulate a 304 Not Modified response on a subsequent request.
    HttpResponse<byte[]> response304 = mock(HttpResponse.class);
    when(response304.statusCode()).thenReturn(304);
    CompletableFuture<HttpResponse<byte[]>> future304 = CompletableFuture.completedFuture(
      response304
    );
    reset(mockHttpClient);
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(future304);

    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> secondCall = prefabHttpClient.requestConfigs(
      0L
    );
    HttpResponse<Supplier<Prefab.Configs>> resp2 = secondCall.get();

    // The second call should use the cached ETag and return the cached response (HIT).
    assertThat(resp2.statusCode()).isEqualTo(200);
    assertThat(resp2.headers().firstValue("X-Cache")).contains("HIT");
    // Verify that parsing the cached value yields the same dummyConfigs.
    Prefab.Configs parsedConfigs = resp2.body().get();
    assertThat(parsedConfigs).isEqualTo(dummyConfigs);

    // Also, verify that the If-None-Match header was added.
    // Capture the HttpRequest used in the second call.
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(
      HttpRequest.class
    );
    verify(mockHttpClient, atLeastOnce()).sendAsync(requestCaptor.capture(), any());
    HttpRequest sentRequest = requestCaptor.getValue();
    assertThat(sentRequest.headers().firstValue("If-None-Match"))
      .contains("etag-no-cache");
  }

  @Test
  void testZeroByteConfigRejectionFromHttpResponse() throws Exception {
    // Mock a 200 response that returns zero bytes
    byte[] zeroBytes = new byte[0];
    HttpResponse<byte[]> zeroByteResponse = mock(HttpResponse.class);
    when(zeroByteResponse.statusCode()).thenReturn(200);
    when(zeroByteResponse.body()).thenReturn(zeroBytes);
    when(zeroByteResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));

    CompletableFuture<HttpResponse<byte[]>> futureZeroBytes = CompletableFuture.completedFuture(
      zeroByteResponse
    );
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(futureZeroBytes);

    // Request configs - this should eventually fail after retries
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> result = prefabHttpClient.requestConfigs(
      0L
    );

    HttpResponse<Supplier<Prefab.Configs>> response = result.get();
    assertThat(response.statusCode()).isEqualTo(200);

    // Try to get the body - this should throw IllegalArgumentException
    try {
      response.body().get();
      assertThat(false)
        .as("Expected IllegalArgumentException for zero-byte config")
        .isTrue();
    } catch (IllegalArgumentException e) {
      // Should get IllegalArgumentException from zero-byte rejection
      assertThat(e.getMessage()).contains("Zero-byte config data is not valid");
    }
  }

  @Test
  void testZeroByteConfigRejectionFromNon200Response() throws Exception {
    // Mock a 404 response that returns zero bytes
    byte[] zeroBytes = new byte[0];
    HttpResponse<byte[]> zeroByteResponse = mock(HttpResponse.class);
    when(zeroByteResponse.statusCode()).thenReturn(404);
    when(zeroByteResponse.body()).thenReturn(zeroBytes);
    when(zeroByteResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));

    CompletableFuture<HttpResponse<byte[]>> futureZeroBytes = CompletableFuture.completedFuture(
      zeroByteResponse
    );
    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(futureZeroBytes);

    // Request configs - this should eventually fail after retries
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> result = prefabHttpClient.requestConfigs(
      0L
    );

    HttpResponse<Supplier<Prefab.Configs>> response = result.get();
    assertThat(response.statusCode()).isEqualTo(404);

    // Try to get the body - this should throw IllegalArgumentException
    try {
      response.body().get();
      assertThat(false)
        .as("Expected IllegalArgumentException for zero-byte config")
        .isTrue();
    } catch (IllegalArgumentException e) {
      // Should get IllegalArgumentException from zero-byte rejection
      assertThat(e.getMessage()).contains("Zero-byte config data is not valid");
    }
  }

  @Test
  void testZeroByteConfigRejectionFromCache() throws Exception {
    // Insert zero-byte data directly into cache
    URI uri = URI.create("http://a.example.com/api/v2/configs/0");
    Field cacheField = HttpClient.class.getDeclaredField("configCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Cache<URI, HttpClient.CacheEntry> cache = (Cache<URI, HttpClient.CacheEntry>) cacheField.get(
      prefabHttpClient
    );

    // Create a cache entry with zero-byte data that is still fresh
    long future = System.currentTimeMillis() + 60_000;
    HttpClient.CacheEntry zeroByteCacheEntry = new HttpClient.CacheEntry(
      new byte[0],
      "zero-byte-etag",
      future
    );
    cache.put(uri, zeroByteCacheEntry);

    // Request configs - should return cached response but fail when accessing body
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> result = prefabHttpClient.requestConfigs(
      0L
    );

    HttpResponse<Supplier<Prefab.Configs>> response = result.get();
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("X-Cache")).contains("HIT");

    // Try to get the body - this should throw IllegalArgumentException
    try {
      response.body().get();
      assertThat(false)
        .as("Expected IllegalArgumentException for zero-byte cached config")
        .isTrue();
    } catch (IllegalArgumentException e) {
      // Should get IllegalArgumentException from zero-byte rejection
      assertThat(e.getMessage()).contains("Zero-byte config data is not valid");
    }
  }

  @Test
  void testValidConfigProcessingAfterZeroByteRejectionImplementation() throws Exception {
    // This test verifies that valid configs can still be processed normally
    // even when zero-byte rejection is in place

    Prefab.Configs validConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(456L)
      )
      .build();
    byte[] validBytes = validConfigs.toByteArray();
    HttpResponse<byte[]> validResponse = mock(HttpResponse.class);
    when(validResponse.statusCode()).thenReturn(200);
    when(validResponse.body()).thenReturn(validBytes);
    when(validResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));

    CompletableFuture<HttpResponse<byte[]>> futureValidBytes = CompletableFuture.completedFuture(
      validResponse
    );

    when(
      mockHttpClient.sendAsync(
        any(HttpRequest.class),
        any(HttpResponse.BodyHandler.class)
      )
    )
      .thenReturn(futureValidBytes);

    // Request configs - should succeed with valid data
    CompletableFuture<HttpResponse<Supplier<Prefab.Configs>>> result = prefabHttpClient.requestConfigs(
      0L
    );

    HttpResponse<Supplier<Prefab.Configs>> response = result.get();
    assertThat(response.statusCode()).isEqualTo(200);

    // Should be able to get valid configs without exception
    Prefab.Configs configs = response.body().get();
    assertThat(configs).isEqualTo(validConfigs);

    // Should have called sendAsync once
    verify(mockHttpClient, times(1)).sendAsync(any(), any());
  }
}
