package ai.timefold.solver.service.maps.service.client.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.impl.recording.RecordingMapService;
import ai.timefold.solver.service.maps.service.client.impl.recording.ReplayMapService;
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
    private final Boolean fallbackEnabled;
    private final Boolean useTraffic;
    private final Optional<String> defaultTimeframeOverride;
    private final TimeframeBucketing timeframeBucketing;
    private final ManagedExecutor managedExecutor;
    private final ObjectMapper mapper;
    private final boolean recordingEnabled;
    private final boolean replayEnabled;
    private final Optional<String> replayDirectory;

    @Inject
    public MapServiceProducer(
            @ConfigProperty(name = "timefold.platform.map-service.use-remote", defaultValue = "true") boolean useRemote,
            HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider,
            HaversineWaypointsProvider waypointsProvider,
            @RestClient MapServiceClient mapService,
            @All List<TravelTimeAndDistanceConverter> converters,
            @ConfigProperty(name = "timefold.platform.map-service.enable-fallback",
                    defaultValue = "false") Boolean fallbackEnabled,
            @ConfigProperty(name = "timefold.platform.map-service.use-traffic", defaultValue = "false") Boolean useTraffic,
            @ConfigProperty(
                    name = "timefold.platform.map-service.default-timeframe") Optional<String> defaultTimeframeOverride,
            TimeframeBucketing timeframeBucketing,
            ManagedExecutor managedExecutor,
            ObjectMapper mapper,
            @ConfigProperty(name = EnvironmentVars.PROPERTY_MAP_SERVICE_RECORDING_ENABLED,
                    defaultValue = "false") boolean recordingEnabled,
            @ConfigProperty(name = "timefold.platform.map-service.replay.enabled",
                    defaultValue = "false") boolean replayEnabled,
            @ConfigProperty(name = "timefold.platform.map-service.replay.directory") Optional<String> replayDirectory) {
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
        this.recordingEnabled = recordingEnabled;
        this.replayEnabled = replayEnabled;
        this.replayDirectory = replayDirectory;
    }

    @Produces
    public MapService mapServiceProducer() {
        var delegate = createMapService();
        // A replay run is never re-recorded, which could overwrite the recordings it reads.
        return recordingEnabled && !replayEnabled
                ? new RecordingMapService(delegate, timeframeBucketing, mapper)
                : delegate;
    }

    private MapService createMapService() {
        if (replayEnabled) {
            var directory = Path.of(replayDirectory.orElseThrow(() -> new IllegalStateException(
                    "The property timefold.platform.map-service.replay.directory is required when "
                            + "timefold.platform.map-service.replay.enabled is true.")));
            return new ReplayMapService(directory, timeframeBucketing,
                    new MapServiceLocalHaversineImpl(travelTimeAndDistanceProvider, waypointsProvider), mapper);
        }
        if (useRemote) {
            return new MapServiceClientImpl(mapService, converters, fallbackEnabled, useTraffic, defaultTimeframeOverride,
                    travelTimeAndDistanceProvider, waypointsProvider, timeframeBucketing, managedExecutor, mapper);
        }
        return new MapServiceLocalHaversineImpl(travelTimeAndDistanceProvider, waypointsProvider);
    }

}
