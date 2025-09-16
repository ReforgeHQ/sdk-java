package com.reforge.sdk.integration;

import static com.reforge.sdk.integration.IntegrationTestFunction.ENABLED;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.reforge.sdk.Sdk;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardIntegrationTestCaseDescriptor
  extends BaseIntegrationTestCaseDescriptor
  implements IntegrationTestCaseDescriptorIF {

  private static final Logger LOG = LoggerFactory.getLogger(
    StandardIntegrationTestCaseDescriptor.class
  );

  private final String clientName;
  private final IntegrationTestFunction function;
  private final IntegrationTestInput input;
  private final IntegrationTestExpectation expected;
  private final Optional<Map<String, Map<String, Object>>> localContextMap;
  private final Optional<Map<String, Map<String, Object>>> blockContextMapMaybe;

  @JsonCreator
  public StandardIntegrationTestCaseDescriptor(
    @JsonProperty("name") String name,
    @JsonProperty("client") String clientName,
    @JsonProperty("function") IntegrationTestFunction function,
    @JsonProperty("client_overrides") IntegrationTestClientOverrides clientOverrides,
    @JsonProperty("input") IntegrationTestInput input,
    @JsonProperty("expected") IntegrationTestExpectation expected,
    @JsonProperty("type") String dataType,
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
    this.clientName = clientName;
    this.function = function;
    this.localContextMap =
      contextMapMaybe.flatMap(cm -> Optional.ofNullable(cm.get("local")));
    this.input = input;
    this.localContextMap.ifPresent(input::setContext);
    this.expected = expected;
    this.dataType = Optional.ofNullable(dataType); // sets on super type
    this.blockContextMapMaybe =
      contextMapMaybe.flatMap(cm -> Optional.ofNullable(cm.get("block")));
  }

  @Override
  protected void performVerification(Sdk sdk) {
    String actualDataType;
    if (dataType.isPresent()) {
      actualDataType = dataType.get();
    } else if (getFunction() == ENABLED) {
      actualDataType = "BOOLEAN";
    } else {
      throw new RuntimeException("no `type` set");
    }

    getExpected()
      .verifyScenario(sdk, getFunction(), getInput(), actualDataType);
  }

  public String getClient() {
    return clientName;
  }

  public IntegrationTestFunction getFunction() {
    return function;
  }

  public IntegrationTestInput getInput() {
    return input;
  }

  public IntegrationTestExpectation getExpected() {
    return expected;
  }

  @Override
  protected ContextSetReadable getBlockContext() {
    return blockContextMapMaybe
      .map(PrefabContextFactory::from)
      .orElse(ContextSet.EMPTY);
  }
}
