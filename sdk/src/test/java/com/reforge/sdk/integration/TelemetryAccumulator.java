package com.reforge.sdk.integration;

import cloud.prefab.domain.Prefab;
import com.reforge.sdk.internal.TelemetryListener;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryAccumulator implements TelemetryListener {

  private static final Logger LOG = LoggerFactory.getLogger(TelemetryAccumulator.class);

  final List<Prefab.TelemetryEvents> telemetryEventsList = new ArrayList<>();

  @Override
  public void telemetryUpload(Prefab.TelemetryEvents telemetryEvents) {
    telemetryEventsList.add(telemetryEvents);
    LOG.info("telemetry events stored for verification: {}", telemetryEvents);
  }

  public List<Prefab.TelemetryEvents> getTelemetryEventsList() {
    return telemetryEventsList;
  }
}
