package ai.timefold.solver.service.maps.service.client.impl;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MapServiceOptionsSupplier {

    private final Optional<String> provider;

    private final Optional<String> location;

    private final Optional<String> model;

    private final Optional<String> modelVersion;

    private final Optional<String> modelResource;

    private final Optional<String> tenantId;

    private final Optional<String> transportType;

    private final Optional<Double> maxDistanceFromRoad;

    public MapServiceOptionsSupplier(
            @ConfigProperty(name = "ai.timefold.platform.map-service.provider") Optional<String> provider,
            @ConfigProperty(name = "ai.timefold.platform.map-service.location") Optional<String> location,
            @ConfigProperty(
                    name = "ai.timefold.platform.map-service.max-distance-from-road") Optional<Double> maxDistanceFromRoad,
            @ConfigProperty(name = "ai.timefold.platform.model") Optional<String> model,
            @ConfigProperty(name = "ai.timefold.platform.model-version") Optional<String> modelVersion,
            @ConfigProperty(name = "ai.timefold.platform.model-resource") Optional<String> modelResource,
            @ConfigProperty(name = "ai.timefold.platform.tenant-id") Optional<String> tenantId,
            @ConfigProperty(name = "ai.timefold.platform.map-service.transport-type") Optional<String> transportType) {
        this.provider = provider;
        this.location = location;
        this.model = model;
        this.modelVersion = modelVersion;
        this.modelResource = modelResource;
        this.tenantId = tenantId;
        this.maxDistanceFromRoad = maxDistanceFromRoad;
        this.transportType = transportType;
    }

    public String getOptions() {
        return getOptions(Optional.empty());
    }

    public String getOptions(Optional<String> locationSetName) {
        String providerOption = provider.map(MapServiceOptions::getProviderOption).orElse("");
        String locationOption = location.map(MapServiceOptions::getLocationOption).orElse("");
        String modelOption = model.map(MapServiceOptions::getModelOption).orElse("");
        String modelVersionOption = modelVersion.map(MapServiceOptions::getModelVersionOption).orElse("");
        String modelResourceOption = modelResource.map(MapServiceOptions::getModelResourceOption).orElse("");
        String tenantIdOption = tenantId.map(MapServiceOptions::getTenantIdOption).orElse("");
        String locationSetNameOption = locationSetName.map(MapServiceOptions::getLocationSetNameOption).orElse("");
        String maxDistanceFromRoadOption = maxDistanceFromRoad.map(MapServiceOptions::getMaxDistanceFromRoadOption).orElse("");
        String transportTypeOption = transportType.map(MapServiceOptions::getTransportTypeOption).orElse("");
        String options = Stream
                .of(providerOption, locationOption, modelOption, modelVersionOption, modelResourceOption, tenantIdOption,
                        locationSetNameOption,
                        maxDistanceFromRoadOption, transportTypeOption)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return options.isEmpty() ? "" : options;
    }

}
