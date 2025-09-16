package com.reforge.sdk.integration;

import org.junit.jupiter.api.function.Executable;

public interface IntegrationTestCaseDescriptorIF {
  String getName();

  Executable asExecutable();
}
