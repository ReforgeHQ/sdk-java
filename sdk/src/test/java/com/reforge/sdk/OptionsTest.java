package com.reforge.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class OptionsTest {

  @Test
  public void testTelemetryDomain() {
    Options options = new Options();
    assertThat(options.getTelemetryHost()).isEqualTo("https://telemetry.reforge.com");

    options = new Options().setTelemetryHost("http://staging-prefab.cloud");
    assertThat(options.getTelemetryHost()).isEqualTo("http://staging-prefab.cloud");
  }

  @Test
  public void testApiDomain() {
    Options options = new Options();
    assertThat(options.getApiHosts()).isEqualTo(Options.DEFAULT_API_HOSTS);
    options = new Options().setApiHosts(List.of("staging-prefab.cloud"));
    assertThat(options.getApiHosts()).isEqualTo(List.of("https://staging-prefab.cloud"));
  }

  @Test
  public void testStreamDomain() {
    Options options = new Options();
    assertThat(options.getStreamHosts()).isEqualTo(Options.DEFAULT_STREAM_HOSTS);
    options = new Options().setApiHosts(List.of("stream.staging-prefab.cloud"));
    assertThat(options.getApiHosts())
      .isEqualTo(List.of("https://stream.staging-prefab.cloud"));
  }

  @Test
  public void apiKeyIsTrimmed() {
    Options options = new Options();
    options.setSdkKey("my-key\n");
    assertThat(options.getSdkKey()).isEqualTo("my-key");
  }
}
