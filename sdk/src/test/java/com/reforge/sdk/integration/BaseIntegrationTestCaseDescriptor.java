package com.reforge.sdk.integration;

import static org.assertj.core.api.Assertions.fail;

import com.google.errorprone.annotations.MustBeClosed;
import com.reforge.sdk.Options;
import com.reforge.sdk.Sdk;
import com.reforge.sdk.context.ContextHelper;
import com.reforge.sdk.context.ContextSet;
import com.reforge.sdk.context.ContextSetReadable;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseIntegrationTestCaseDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(
    BaseIntegrationTestCaseDescriptor.class
  );
  private final String name;
  private final IntegrationTestClientOverrides clientOverrides;
  private final Optional<ContextSet> globalContextMaybe;
  protected Optional<String> dataType;

  public BaseIntegrationTestCaseDescriptor(
    String name,
    IntegrationTestClientOverrides clientOverrides,
    Optional<ContextSet> globalContextMaybe
  ) {
    this.name = name;
    this.clientOverrides = clientOverrides;
    this.globalContextMaybe = globalContextMaybe;
  }

  public String getName() {
    return name;
  }

  protected abstract void performVerification(Sdk sdk);

  private static final List<String> REQUIRED_ENV_VARS = List.of(
    "REFORGE_INTEGRATION_TEST_SDK_KEY",
    "REFORGE_INTEGRATION_TEST_ENCRYPTION_KEY",
    "NOT_A_NUMBER",
    "IS_A_NUMBER"
  );

  protected ContextSetReadable getBlockContext() {
    return ContextSet.EMPTY;
  }

  public Executable asExecutable() {
    return () -> {
      for (String requiredEnvVar : REQUIRED_ENV_VARS) {
        if (System.getenv(requiredEnvVar) == null) {
          fail(
            "Environment variable %s must be set. Please see README for required setup",
            requiredEnvVar
          );
        }
      }
      try (Sdk client = buildClient(clientOverrides)) {
        ContextHelper helper = new ContextHelper(client.configClient());
        try (
          ContextHelper.PrefabContextScope ignored = helper.performWorkWithAutoClosingContext(
            getBlockContext()
          )
        ) {
          performVerification(client);
        }
      }
    };
  }

  @MustBeClosed
  private Sdk buildClient(IntegrationTestClientOverrides clientOverrides) {
    String apiKey = System.getenv("REFORGE_INTEGRATION_TEST_SDK_KEY");
    if (apiKey == null) {
      throw new IllegalStateException(
        "Env var REFORGE_INTEGRATION_TEST_SDK_KEY is not set"
      );
    }

    Options options = new Options()
      .setSdkKey(apiKey)
      .setTelemetryHost("https://telemetry.goatsofreforge.com")
      .setApiHosts(List.of("https://api.goatsofreforge.com"))
      .setStreamHosts(List.of("https://stream.goatsofreforge.com"))
      .setInitializationTimeoutSec(2000);
    clientOverrides
      .getInitTimeoutSeconds()
      .ifPresent(options::setInitializationTimeoutSec);
    clientOverrides
      .getReforgeApiUrl()
      .ifPresent(host -> options.setApiHosts(List.of(host)));
    clientOverrides.getOnInitFailure().ifPresent(options::setOnInitializationFailure);
    clientOverrides.getContextUploadMode().ifPresent(options::setContextUploadMode);
    globalContextMaybe.ifPresent(options::setGlobalContext);

    if (clientOverrides.getAggregator().isPresent()) {
      LOG.error("clientOverrides-aggregator is not yet supported");
    }
    if (clientOverrides.getOnNoDefault().isPresent()) {
      LOG.error("clientOverrides-onNoDefault is not yet supported");
    }

    customizeOptions(options);
    return new Sdk(options);
  }

  protected void customizeOptions(Options options) {}
}
