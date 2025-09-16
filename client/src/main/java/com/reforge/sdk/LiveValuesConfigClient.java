package com.reforge.sdk;

import com.reforge.sdk.context.ContextStore;
import com.reforge.sdk.context.Context;
import com.reforge.sdk.value.Value;
import java.time.Duration;
import java.util.List;

public interface LiveValuesConfigClient {
  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a value that will be evaluated at runtime based on the context from the current scope
   */
  Value<String> liveString(String key);

  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a value that will be evaluated at runtime based on the context from the current scope
   */
  Value<List<String>> liveStringList(String key);

  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a value that will be evaluated at runtime based on the context from the current scope
   */
  Value<Boolean> liveBoolean(String key);

  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a value that will be evaluated at runtime based on the context from the current scope
   */
  Value<Long> liveLong(String key);

  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a value that will be evaluated at runtime based on the context from the current scope
   */
  Value<Double> liveDouble(String key);

  /**
   * Evaluates a configuration based on context from the ContextStore
   * ie set via {@link ContextStore#addContext(Context) addContext}
   * @param key name of the config to evaluate
   * @return a Duration value that will be evaluated at runtime based on the context from the current scope
   */
  Value<Duration> liveDuration(String key);
}
