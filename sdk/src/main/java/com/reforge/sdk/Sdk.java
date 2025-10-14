package com.reforge.sdk;

import com.reforge.sdk.internal.ConfigClientImpl;
import com.reforge.sdk.internal.FeatureFlagClientImpl;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sdk implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(Sdk.class);

  private final Options options;
  private ConfigClientImpl configClient;
  private FeatureFlagClient featureFlagClient;
  private final AtomicBoolean closed;

  public Sdk(Options options) {
    this.options = options;

    if (options.isLocalOnly()) {
      LOG.info("Initializing Reforge SDK LocalOnly");
    } else if (options.isLocalDatafileMode()) {
      LOG.info("Initializing Reforge SDK from local file {}", options.getLocalDatafile());
    } else {
      if (options.getSdkKey() == null || options.getSdkKey().isEmpty()) {
        throw new RuntimeException("REFORGE_BACKEND_SDK_KEY not set");
      }
      LOG.info("Initializing Reforge SDK for sdkKeyId {}", options.getApiKeyId());
    }

    this.closed = new AtomicBoolean(false);
  }

  public ConfigClient configClient() {
    return configClientImpl();
  }

  private ConfigClientImpl configClientImpl() {
    if (configClient == null) {
      synchronized (this) {
        if (configClient == null) {
          configClient = new ConfigClientImpl(this);
        }
      }
    }
    return configClient;
  }

  public FeatureFlagClient featureFlagClient() {
    if (featureFlagClient == null) {
      synchronized (this) {
        if (featureFlagClient == null) {
          featureFlagClient = new FeatureFlagClientImpl(configClientImpl());
        }
      }
    }
    return featureFlagClient;
  }

  public Options getOptions() {
    return options;
  }

  @Override
  public void close() {
    if (closed.get()) {
      return;
    }

    synchronized (this) {
      if (!closed.get()) {
        closed.set(true);
      }
    }
  }
}
