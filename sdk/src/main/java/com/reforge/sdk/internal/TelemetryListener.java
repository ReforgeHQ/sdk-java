package com.reforge.sdk.internal;

import cloud.prefab.domain.Prefab;

/**
 * this is for Prefab integration test use; consider unsupported
 */
@Internal
public interface TelemetryListener {
  void telemetryUpload(Prefab.TelemetryEvents telemetryEvents);
}
