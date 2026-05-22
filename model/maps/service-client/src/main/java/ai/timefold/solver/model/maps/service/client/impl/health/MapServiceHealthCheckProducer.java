package ai.timefold.solver.model.maps.service.client.impl.health;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Liveness;

@Dependent
public class MapServiceHealthCheckProducer {

    private boolean useRemote;
    private final MapServiceAvailabilityProbe mapServiceAvailabilityProbe;

    public MapServiceHealthCheckProducer(
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-remote", defaultValue = "true") boolean useRemote,
            MapServiceAvailabilityProbe mapServiceAvailabilityProbe) {
        this.useRemote = useRemote;
        this.mapServiceAvailabilityProbe = mapServiceAvailabilityProbe;
    }

    @Produces
    @Singleton
    @Liveness
    public MapServiceHealthCheck mapServiceHealthCheck() {
        if (useRemote) {
            return new MapServiceHealthCheck(mapServiceAvailabilityProbe);
        }
        return null;
    }

}
