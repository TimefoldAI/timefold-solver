package ai.timefold.solver.service.maps.service.client.util;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceClient;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceClientImpl;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceLocalHaversineImpl;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverter;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;

@Dependent
@Alternative
@Priority(1)
public class MapServiceWithWrapperProducer {

    private final boolean useRemote;
    private final HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider;
    private final HaversineWaypointsProvider waypointsProvider;
    private final MapServiceClient mapService;
    private final List<TravelTimeAndDistanceConverter> converters;
    private final Boolean fallbackEnabled;
    private final TimeframeBucketing timeframeBucketing;
    private final ManagedExecutor managedExecutor;
    private final ObjectMapper mapper;
    private final MapServiceInvocationCounter mapServiceInvocationCounter;

    @Inject
    public MapServiceWithWrapperProducer(
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-remote", defaultValue = "true") boolean useRemote,
            HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider,
            HaversineWaypointsProvider waypointsProvider,
            @RestClient MapServiceClient mapService,
            @All List<TravelTimeAndDistanceConverter> converters,
            @ConfigProperty(name = "ai.timefold.platform.map-service.enable-fallback",
                    defaultValue = "false") Boolean fallbackEnabled,
            TimeframeBucketing timeframeBucketing,
            ManagedExecutor managedExecutor,
            ObjectMapper mapper,
            MapServiceInvocationCounter mapServiceInvocationCounter) {
        this.useRemote = useRemote;
        this.travelTimeAndDistanceProvider = travelTimeAndDistanceProvider;
        this.waypointsProvider = waypointsProvider;
        this.mapService = mapService;
        this.converters = converters;
        this.fallbackEnabled = fallbackEnabled;
        this.timeframeBucketing = timeframeBucketing;
        this.managedExecutor = managedExecutor;
        this.mapServiceInvocationCounter = mapServiceInvocationCounter;
        this.mapper = mapper;
    }

    @Produces
    public MapService mapServiceProducer() {
        MapService mapService;

        if (useRemote) {
            mapService = new MapServiceClientImpl(this.mapService, converters, fallbackEnabled, false,
                    "", travelTimeAndDistanceProvider, waypointsProvider, timeframeBucketing,
                    managedExecutor, mapper);
        } else {
            mapService = new MapServiceLocalHaversineImpl(travelTimeAndDistanceProvider, waypointsProvider);
        }
        return new MapServiceTestWrapper(mapService, mapServiceInvocationCounter);
    }

}
