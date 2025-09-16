package com.reforge.sdk;

public class SdkInitializationTimeoutException extends RuntimeException {

  public SdkInitializationTimeoutException(int initializationTimeoutSec) {
    super("Sdk Failed to Initialize after " + initializationTimeoutSec + " seconds");
  }
}
