package com.reforge.sdk.integration.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import cloud.prefab.domain.Prefab;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.reforge.sdk.Options;
import com.reforge.sdk.Sdk;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.integration.IntegrationTestClientOverrides;
import com.reforge.sdk.integration.IntegrationTestFunction;
import com.reforge.sdk.integration.PrefabContextFactory;
import com.reforge.sdk.integration.TelemetryAccumulator;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleContextIntegrationTestCaseDescriptor
  extends TelemetryIntegrationTestCaseDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(
    ExampleContextIntegrationTestCaseDescriptor.class
  );
  private final JsonNode dataNode;
  private final JsonNode expectedDataNode;

  @JsonCreator
  public ExampleContextIntegrationTestCaseDescriptor(
    @JsonProperty("name") String name,
    @JsonProperty("client") String client,
    @JsonProperty("client_overrides") IntegrationTestClientOverrides clientOverrides,
    @JsonProperty("endpoint") String endpoint,
    @JsonProperty("function") IntegrationTestFunction function,
    @JsonProperty("aggregator") String aggregator,
    @JsonProperty("data") JsonNode dataNode,
    @JsonProperty("expected_data") JsonNode expectedDataNode,
    @JsonProperty(
      "contexts"
    ) Optional<Map<String, Map<String, Map<String, Object>>>> contextMapMaybe
  ) {
    super(
      name,
      MoreObjects.firstNonNull(clientOverrides, IntegrationTestClientOverrides.empty()),
      contextMapMaybe
        .map(contextMap -> contextMap.get("global"))
        .map(PrefabContextFactory::from)
        .map(ContextSet::convert)
    );
    this.dataNode = dataNode;
    this.expectedDataNode = expectedDataNode;
  }

  @Override
  protected void customizeOptions(Options options) {
    super.customizeOptions(options);
    options.setContextUploadMode(Options.CollectContextMode.PERIODIC_EXAMPLE);
  }

  @Override
  protected void performVerification(Sdk sdk) {
    ContextSet contextSetToSend = buildContextFromJsonNode(dataNode);
    ContextSet expectedContextSet = buildContextFromJsonNode(expectedDataNode);

    sdk.configClient().get("my-test-key", contextSetToSend);
    TelemetryAccumulator telemetryAccumulator = getTelemetryAccumulator(sdk);
    await()
      .atMost(Duration.of(3, ChronoUnit.SECONDS))
      .untilAsserted(() -> {
        List<Prefab.Context> actualContexts = telemetryAccumulator
          .getTelemetryEventsList()
          .stream()
          .flatMap(t -> t.getEventsList().stream())
          .filter(Prefab.TelemetryEvent::hasExampleContexts)
          .map(Prefab.TelemetryEvent::getExampleContexts)
          .flatMap(c -> c.getExamplesList().stream())
          .flatMap(e -> e.getContextSet().getContextsList().stream())
          .filter(c -> !c.getType().equals("prefab-api-key")) // ignore the context sent by api
          .collect(Collectors.toList());

        assertThat(actualContexts)
          .containsExactlyInAnyOrderElementsOf(
            expectedContextSet.toProto().getContextsList()
          );

        LOG.info("Actual contexts were {}", actualContexts);
      });
  }

  ContextSet buildContextFromJsonNode(JsonNode dataNode) {
    ContextSet contextSet = new ContextSet();

    switch (dataNode.getNodeType()) {
      case NULL:
        // nothing to do, context is empty
        break;
      case OBJECT:
        ObjectNode objectNode = (ObjectNode) dataNode;
        objectNode
          .fields()
          .forEachRemaining(keyValuePair ->
            contextSet.addContext(
              buildContextFromObjectDataNode(
                keyValuePair.getKey(),
                keyValuePair.getValue()
              )
            )
          );
        break;
      case ARRAY:
        dataNode.forEach(arrayMemberNode -> {
          assertThat(arrayMemberNode.isObject()).as("Array members should be an object");
          // this is a single key object node
          arrayMemberNode
            .fields()
            .forEachRemaining(keyValuePair ->
              contextSet.addContext(
                buildContextFromObjectDataNode(
                  keyValuePair.getKey(),
                  keyValuePair.getValue()
                )
              )
            );
        });
        break;
      default:
        fail("Unexpected node type %s", dataNode.getNodeType());
    }
    return contextSet;
  }
}
