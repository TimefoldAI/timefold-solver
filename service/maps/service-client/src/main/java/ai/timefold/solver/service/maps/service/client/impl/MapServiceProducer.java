package ai.timefold.solver.service.maps.service.client.impl;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverter;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;

@Dependent
public class MapServiceProducer {

    private final boolean useRemote;
    private final HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider;
    private final HaversineWaypointsProvider waypointsProvider;
    private final MapServiceClient mapService;
    private final List<TravelTimeAndDistanceConverter> converters;
    private final Optional<Boolean> fallbackEnabled;
    private final Optional<Boolean> useTraffic;
    private final Optional<String> defaultTimeframeOverride;
    private final TimeframeBucketing timeframeBucketing;
    private final ManagedExecutor managedExecutor;
    private final ObjectMapper mapper;

    @Inject
    public MapServiceProducer(
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-remote", defaultValue = "true") boolean useRemote,
            HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider,
            HaversineWaypointsProvider waypointsProvider,
            @RestClient MapServiceClient mapService,
            @All List<TravelTimeAndDistanceConverter> converters,
            @ConfigProperty(name = "ai.timefold.platform.map-service.enable-fallback") Optional<Boolean> fallbackEnabled,
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-traffic") Optional<Boolean> useTraffic,
            @ConfigProperty(
                    name = "ai.timefold.platform.map-service.default-timeframe") Optional<String> defaultTimeframeOverride,
            TimeframeBucketing timeframeBucketing,
            ManagedExecutor managedExecutor,
            ObjectMapper mapper) {
        this.useRemote = useRemote;
        this.travelTimeAndDistanceProvider = travelTimeAndDistanceProvider;
        this.waypointsProvider = waypointsProvider;
        this.mapService = mapService;
        this.converters = converters;
        this.fallbackEnabled = fallbackEnabled;
        this.useTraffic = useTraffic;
        this.defaultTimeframeOverride = defaultTimeframeOverride;
        this.timeframeBucketing = timeframeBucketing;
        this.managedExecutor = managedExecutor;
        this.mapper = mapper;
    }

    @Produces
    public MapService mapServiceProducer() {
        if (useRemote) {
            return new MapServiceClientImpl(mapService, converters, fallbackEnabled, useTraffic, defaultTimeframeOverride,
                    travelTimeAndDistanceProvider, waypointsProvider, timeframeBucketing, managedExecutor, mapper);
        }
        return new MapServiceLocalHaversineImpl(travelTimeAndDistanceProvider, waypointsProvider);
    }

}
