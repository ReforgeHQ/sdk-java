package com.reforge.sdk.internal;

import java.util.Optional;

public class SystemEnvVarLookup implements EnvironmentVariableLookup {

  public Optional<String> get(String name) {
    return Optional.ofNullable(System.getenv(name));
  }
}
