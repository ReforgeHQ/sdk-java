package com.reforge.sdk.config;

import com.reforge.sdk.Options;
import com.reforge.sdk.Sdk;

public class TestData {

  public enum TestDataConfigSet {
    SPECIFIC_LOGGING("logging_specific"),
    DEFAULT_LOGGING("logging_default");

    private final String environmentName;

    TestDataConfigSet(String environmentName) {
      this.environmentName = environmentName;
    }

    public String getEnvironmentName() {
      return environmentName;
    }
  }

  public static Options getDefaultOptionsWithEnvName(String envName) {
    return new Options().setDatasource(Options.Datasources.LOCAL_ONLY);
  }

  /*
  public Options getDefaultOptionsLoggingConfiguration(
    TestDataConfigSet testDataConfigSet
  ) {
    return getDefaultOptionsLoggingConfiguration(testDataConfigSet.getEnvironmentName());
  }

  public static PrefabCloudClient clientWithSpecificLogLevel() {
    return clientWithOptions()
  }

  public static PrefabCloudClient clientWithDefaultLogLevel() {

    return ("logging_default");
  }



 */
  public static Sdk clientWithOptions(Options options) {
    return new Sdk(options);
  }
}
