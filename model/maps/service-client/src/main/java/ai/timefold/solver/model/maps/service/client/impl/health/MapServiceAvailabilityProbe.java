package ai.timefold.solver.model.maps.service.client.impl.health;

import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import ai.timefold.solver.model.maps.service.client.impl.MapServiceClient;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

public class MapServiceAvailabilityProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapServiceAvailabilityProbe.class);
    private static final String MAP_SERVICE_URL_NOT_SET = "NOT SET";

    private final boolean useRemote;
    private final MapServiceClient mapService;
    private final Optional<String> mapServiceUrl;

    @Inject
    public MapServiceAvailabilityProbe(
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-remote", defaultValue = "true") boolean useRemote,
            @RestClient MapServiceClient mapService,
            @ConfigProperty(name = "ai.timefold.platform.map-service.url") Optional<String> mapServiceUrl) {
        this.useRemote = useRemote;
        this.mapService = mapService;
        this.mapServiceUrl = mapServiceUrl;
    }

    public void verifyOnStart(@Observes StartupEvent event) {
        if (useRemote) {
            boolean available = check();
            if (available) {
                LOGGER.info("Map service is available and accepts requests");
            } else {
                throw new IllegalStateException(
                        "Map service is not available but model is configured (via 'ai.timefold.platform.map-service.url' property) to use it. Make sure Map Service is running and is accessible at "
                                + mapServiceUrl());
            }
        }
    }

    public boolean check() {
        try {
            return mapService.health().getStatus() == 200;
        } catch (Exception e) {
            LOGGER.error("Map service call failed due to {}", e.getMessage(), e);
            return false;
        }
    }

    public String mapServiceUrl() {
        return mapServiceUrl.orElse(MAP_SERVICE_URL_NOT_SET);
    }

}
