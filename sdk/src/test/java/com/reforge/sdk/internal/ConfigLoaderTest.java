package com.reforge.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.ConfigClient;
import com.reforge.sdk.Options;
import com.reforge.sdk.config.ConfigElement;
import com.reforge.sdk.config.Provenance;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.context.ContextSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ConfigLoaderTest {

  private ConfigLoader configLoader;

  void buildLoaderWithOptions(Options options) {
    configLoader = new ConfigLoader(options);
  }

  @Nested
  class JsonFileNoOverrideTests {

    @BeforeEach
    void beforeEach() {
      buildLoaderWithOptions(
        new Options()
          .setLocalDatafile("src/test/resources/prefab.Development.5.config.json")
      );
    }

    @Test
    void itIgnoresOverrides() {
      configLoader.setConfigs(
        configLoader.loadFromJsonFile(),
        new Provenance(ConfigClient.Source.LOCAL_FILE)
      );
      Optional<Prefab.Config> configFromJsonFileMaybe = getConfig("cool.bool.enabled");
      assertThat(configFromJsonFileMaybe).isPresent();
      assertThat(configFromJsonFileMaybe.get().hasChangedBy()).isTrue();
    }
  }

  @Nested
  class UnitTestEnvTests {

    @BeforeEach
    void beforeEach() {
      buildLoaderWithOptions(new Options());
    }

    @Test
    public void testHighwater() {
      assertThat(configLoader.getHighwaterMark()).isEqualTo(0);

      configLoader.set(cd(1, "sample_int", 456));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(1);

      configLoader.set(cd(5, "sample_int", 456));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(5);

      configLoader.set(cd(3, "sample_int", 456));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(5);
    }

    @Test
    public void testKeepsMostRecent() {
      assertThat(configLoader.getHighwaterMark()).isEqualTo(0);

      configLoader.set(cd(1, "sample_int", 1));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(1);
      assertThat(getValue("sample_int")).map(Prefab.ConfigValue::getInt).contains(1L);

      configLoader.set(cd(4, "sample_int", 4));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(4);
      assertThat(getValue("sample_int")).map(Prefab.ConfigValue::getInt).contains(4L);

      // next value is not kept because its highwater mark is 2
      configLoader.set(cd(2, "sample_int", 2));
      assertThat(configLoader.getHighwaterMark()).isEqualTo(4);
      assertThat(getValue("sample_int")).map(Prefab.ConfigValue::getInt).contains(4L);
    }

    @Test
    public void testLoadingTombstonesRemoves() {
      assertThat(configLoader.calcConfig().getConfigs().get("val_from_api")).isNull();

      configLoader.set(cd(2, "val_from_api", 456));
      assertThat(getValue("val_from_api"))
        .map(Prefab.ConfigValue::getInt)
        .isPresent()
        .get()
        .isEqualTo(456L);

      configLoader.set(
        new ConfigElement(
          Prefab.Config.newBuilder().setId(2).setKey("val_from_api").build(),
          new Provenance(ConfigClient.Source.LOCAL_ONLY, "unit_tests")
        )
      );
      assertThat(configLoader.calcConfig().getConfigs().get("val_from_api")).isNull();
    }
  }

  @Nested
  class ContextLoadingTests {

    final ContextSet globalContext = ContextSet.from(
      Context.newBuilder("deploy").put("name", "prefab-api").build()
    );

    @BeforeEach
    void beforeEach() {
      buildLoaderWithOptions(new Options().setGlobalContext(globalContext));
    }

    @Test
    void itReturnsGlobalContextFromCalcConfigAndEmptyApiDefaultContext() {
      MergedConfigData mergedConfigData = configLoader.calcConfig();
      assertThat(mergedConfigData.getGlobalContextSet()).isEqualTo(globalContext);

      assertThat(mergedConfigData.getConfigIncludedContext().isEmpty()).isTrue();
    }

    @Test
    void itReturnsGlobalContextAndApiDefaultContextFromCalcConfig() {
      ContextSet apiDefaultContext = ContextSet.from(
        Context
          .newBuilder("fruitStand")
          .put("applePrice", 1)
          .put("peachPrice", 2)
          .build(),
        Context.newBuilder("weather").put("highTemp", 82).build()
      );

      configLoader.setConfigs(
        Prefab.Configs
          .newBuilder()
          .setDefaultContext(apiDefaultContext.toProto())
          .build(),
        new Provenance(ConfigClient.Source.STREAMING)
      );

      MergedConfigData mergedConfigData = configLoader.calcConfig();
      assertThat(mergedConfigData.getGlobalContextSet()).isEqualTo(globalContext);

      assertThat(mergedConfigData.getConfigIncludedContext())
        .isEqualTo(apiDefaultContext);
    }
  }

  private ConfigElement cd(int id, String key, int val) {
    return new ConfigElement(
      Prefab.Config
        .newBuilder()
        .setId(id)
        .setKey(key)
        .addRows(
          Prefab.ConfigRow
            .newBuilder()
            .addValues(
              Prefab.ConditionalValue
                .newBuilder()
                .setValue(Prefab.ConfigValue.newBuilder().setInt(val).build())
                .build()
            )
        )
        .build(),
      new Provenance(ConfigClient.Source.LOCAL_ONLY, "unit_tests")
    );
  }

  private void assertValueOfConfigIs(String expectedValue, String configKey) {
    assertThat(getValue(configKey))
      .map(Prefab.ConfigValue::getString)
      .get()
      .isEqualTo(expectedValue);
  }

  private void assertValueOfConfigIsEmpty(String configKey) {
    assertThat(getValue(configKey)).isEmpty();
  }

  private void assertValueOfConfigIsLogLevel(
    Prefab.LogLevel expectedValue,
    String configKey
  ) {
    assertThat(getValue(configKey))
      .map(Prefab.ConfigValue::getLogLevel)
      .contains(expectedValue);
  }

  private Optional<Prefab.Config> getConfig(String configKey) {
    return Optional
      .ofNullable(configLoader.calcConfig().getConfigs().get(configKey))
      .map(ConfigElement::getConfig);
  }

  private Optional<Prefab.ConfigValue> getValue(String configKey) {
    return getConfig(configKey)
      .map(config -> config.getRowsList().get(0).getValues(0).getValue());
  }
}
