package com.reforge.sdk.util;

import java.util.concurrent.ExecutionException;

public interface Cache {
  byte[] get(String s) throws ExecutionException, InterruptedException;

  void set(String key, int expiryInSeconds, byte[] bytes);
}
