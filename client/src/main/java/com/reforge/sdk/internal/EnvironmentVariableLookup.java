package com.reforge.sdk.internal;

import java.util.Optional;

public interface EnvironmentVariableLookup {
  Optional<String> get(String name);
}
