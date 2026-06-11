package ai.timefold.solver.service.maps.service.client.impl.health;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class MapServiceHealthCheck implements HealthCheck {

    private final MapServiceAvailabilityProbe mapServiceAvailabilityProbe;

    @Inject
    public MapServiceHealthCheck(MapServiceAvailabilityProbe mapServiceAvailabilityProbe) {
        this.mapServiceAvailabilityProbe = mapServiceAvailabilityProbe;
    }

    @Override
    public HealthCheckResponse call() {
        if (mapServiceAvailabilityProbe.check()) {
            return HealthCheckResponse.up("Timefold - Map Service - liveness check");
        } else {
            return HealthCheckResponse.named("Timefold - Map Service - liveness check")
                    .down()
                    .withData("url", mapServiceAvailabilityProbe.mapServiceUrl())
                    .build();
        }
    }
}
