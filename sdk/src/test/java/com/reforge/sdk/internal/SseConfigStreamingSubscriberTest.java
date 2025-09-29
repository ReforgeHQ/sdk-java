package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import cloud.prefab.domain.Prefab;
import cloud.prefab.sse.events.DataEvent;
import java.util.Base64;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseConfigStreamingSubscriberTest {

  @Mock
  HttpClient mockHttpClient;

  @Mock
  Supplier<Long> mockHighwaterMarkSupplier;

  @Mock
  Consumer<Prefab.Configs> mockConfigsConsumer;

  @Mock
  ScheduledExecutorService mockScheduledExecutorService;

  SseConfigStreamingSubscriber.FlowSubscriber flowSubscriber;

  @BeforeEach
  void setup() {
    // Setup method - mocks will be configured in individual tests as needed
  }

  @Test
  void testZeroByteDataEventIgnored() {
    // Create a FlowSubscriber directly to test zero-byte handling
    Consumer<Boolean> mockRestartHandler = mock(Consumer.class);
    flowSubscriber =
      new SseConfigStreamingSubscriber.FlowSubscriber(
        mockConfigsConsumer,
        mockRestartHandler
      );

    // Mock the subscription
    Flow.Subscription mockSubscription = mock(Flow.Subscription.class);
    flowSubscriber.onSubscribe(mockSubscription);

    // Create a DataEvent with zero-byte base64 data (empty string encodes to zero bytes)
    String emptyBase64 = Base64.getEncoder().encodeToString(new byte[0]);
    DataEvent zeroByteDataEvent = new DataEvent("config", emptyBase64, null);

    // Process the zero-byte data event
    flowSubscriber.onNext(zeroByteDataEvent);

    // Verify that the config consumer was never called (zero bytes should be ignored)
    verify(mockConfigsConsumer, never()).accept(any(Prefab.Configs.class));

    // Verify that hasReceivedData is still true (we did receive data, just ignored it)
    assertThat(flowSubscriber.getHasReceivedData()).isTrue();

    // Verify that subscription.request(1) was called twice (once for onSubscribe, once for onNext)
    verify(mockSubscription, times(2)).request(1);
  }

  @Test
  void testValidDataEventProcessed() throws Exception {
    // Create a FlowSubscriber directly to test valid data handling
    Consumer<Boolean> mockRestartHandler = mock(Consumer.class);
    flowSubscriber =
      new SseConfigStreamingSubscriber.FlowSubscriber(
        mockConfigsConsumer,
        mockRestartHandler
      );

    // Mock the subscription
    Flow.Subscription mockSubscription = mock(Flow.Subscription.class);
    flowSubscriber.onSubscribe(mockSubscription);

    // Create valid config data
    Prefab.Configs validConfigs = Prefab.Configs
      .newBuilder()
      .setConfigServicePointer(
        Prefab.ConfigServicePointer.newBuilder().setProjectId(123L).setProjectEnvId(456L)
      )
      .build();
    byte[] validBytes = validConfigs.toByteArray();
    String validBase64 = Base64.getEncoder().encodeToString(validBytes);

    DataEvent validDataEvent = new DataEvent("config", validBase64, null);

    // Process the valid data event
    flowSubscriber.onNext(validDataEvent);

    // Verify that the config consumer was called with the parsed configs
    ArgumentCaptor<Prefab.Configs> configsCaptor = ArgumentCaptor.forClass(
      Prefab.Configs.class
    );
    verify(mockConfigsConsumer, times(1)).accept(configsCaptor.capture());

    Prefab.Configs capturedConfigs = configsCaptor.getValue();
    assertThat(capturedConfigs.getConfigServicePointer().getProjectId()).isEqualTo(123L);
    assertThat(capturedConfigs.getConfigServicePointer().getProjectEnvId())
      .isEqualTo(456L);

    // Verify that hasReceivedData is true
    assertThat(flowSubscriber.getHasReceivedData()).isTrue();
  }

  @Test
  void testEmptyConfigKeepAliveIgnored() throws Exception {
    // Create a FlowSubscriber directly to test keep-alive handling
    Consumer<Boolean> mockRestartHandler = mock(Consumer.class);
    flowSubscriber =
      new SseConfigStreamingSubscriber.FlowSubscriber(
        mockConfigsConsumer,
        mockRestartHandler
      );

    // Mock the subscription
    Flow.Subscription mockSubscription = mock(Flow.Subscription.class);
    flowSubscriber.onSubscribe(mockSubscription);

    // Create empty config data (no ConfigServicePointer)
    Prefab.Configs emptyConfigs = Prefab.Configs.newBuilder().build();
    byte[] emptyBytes = emptyConfigs.toByteArray();
    String emptyBase64 = Base64.getEncoder().encodeToString(emptyBytes);

    DataEvent keepAliveEvent = new DataEvent("config", emptyBase64, null);

    // Process the keep-alive event
    flowSubscriber.onNext(keepAliveEvent);

    // Verify that the config consumer was never called (keep-alive should be ignored)
    verify(mockConfigsConsumer, never()).accept(any(Prefab.Configs.class));

    // Verify that hasReceivedData is true (we did receive data)
    assertThat(flowSubscriber.getHasReceivedData()).isTrue();
  }

  @Test
  void testEmptyDataPayloadIgnored() {
    // Create a FlowSubscriber directly to test empty payload handling
    Consumer<Boolean> mockRestartHandler = mock(Consumer.class);
    flowSubscriber =
      new SseConfigStreamingSubscriber.FlowSubscriber(
        mockConfigsConsumer,
        mockRestartHandler
      );

    // Mock the subscription
    Flow.Subscription mockSubscription = mock(Flow.Subscription.class);
    flowSubscriber.onSubscribe(mockSubscription);

    // Create a DataEvent with empty data payload
    DataEvent emptyDataEvent = new DataEvent("config", "", null);

    // Process the empty data event
    flowSubscriber.onNext(emptyDataEvent);

    // Verify that the config consumer was never called
    verify(mockConfigsConsumer, never()).accept(any(Prefab.Configs.class));

    // Verify that hasReceivedData is still true (we did receive data, just ignored it)
    assertThat(flowSubscriber.getHasReceivedData()).isTrue();
  }

  @Test
  void testWhitespaceOnlyDataPayloadIgnored() {
    // Create a FlowSubscriber directly to test whitespace-only payload handling
    Consumer<Boolean> mockRestartHandler = mock(Consumer.class);
    flowSubscriber =
      new SseConfigStreamingSubscriber.FlowSubscriber(
        mockConfigsConsumer,
        mockRestartHandler
      );

    // Mock the subscription
    Flow.Subscription mockSubscription = mock(Flow.Subscription.class);
    flowSubscriber.onSubscribe(mockSubscription);

    // Create a DataEvent with whitespace-only data payload
    DataEvent whitespaceDataEvent = new DataEvent("config", "   \n\t  ", null);

    // Process the whitespace data event
    flowSubscriber.onNext(whitespaceDataEvent);

    // Verify that the config consumer was never called
    verify(mockConfigsConsumer, never()).accept(any(Prefab.Configs.class));

    // Verify that hasReceivedData is still true (we did receive data, just ignored it)
    assertThat(flowSubscriber.getHasReceivedData()).isTrue();
  }
}
