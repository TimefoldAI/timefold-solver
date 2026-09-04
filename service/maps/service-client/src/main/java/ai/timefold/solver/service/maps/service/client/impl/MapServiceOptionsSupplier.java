package ai.timefold.solver.service.maps.service.client.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.maps.api.model.TransportType;
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

    private final Optional<Double> maxDistanceFromRoad;

    private final List<TransportType> transportTypes;

    public MapServiceOptionsSupplier(
            @ConfigProperty(name = "timefold.platform.map-service.provider") Optional<String> provider,
            @ConfigProperty(name = "timefold.platform.map-service.location") Optional<String> location,
            @ConfigProperty(
                    name = "timefold.platform.map-service.max-distance-from-road") Optional<Double> maxDistanceFromRoad,
            @ConfigProperty(name = "timefold.model.id") Optional<String> model,
            @ConfigProperty(name = "timefold.model.api-version") Optional<String> modelVersion,
            @ConfigProperty(name = "timefold.model.rest-resource") Optional<String> modelResource,
            @ConfigProperty(name = "timefold.platform.tenant-id") Optional<String> tenantId,
            @ConfigProperty(name = "timefold.platform.map-service.transport-type") Optional<String> transportType) {
        this.provider = provider;
        this.location = location;
        this.model = model;
        this.modelVersion = modelVersion;
        this.modelResource = modelResource;
        this.tenantId = tenantId;
        this.maxDistanceFromRoad = maxDistanceFromRoad;
        this.transportTypes = resolveTransportTypes(transportType);
    }

    public List<TransportType> getTransportTypes() {
        return transportTypes;
    }

    public String getOptions() {
        return getOptions(Optional.empty());
    }

    public String getOptions(Optional<String> locationSetName) {
        // Legacy single-mode callers get the primary transport type.
        return getOptions(locationSetName, transportTypes.get(0));
    }

    public String getOptions(TransportType transportType) {
        return getOptions(Optional.empty(), transportType);
    }

    public String getOptions(Optional<String> locationSetName, TransportType transportType) {
        String providerOption = provider.map(MapServiceOptions::getProviderOption).orElse("");
        String locationOption = location.map(MapServiceOptions::getLocationOption).orElse("");
        String modelOption = model.map(MapServiceOptions::getModelOption).orElse("");
        String modelVersionOption = modelVersion.map(MapServiceOptions::getModelVersionOption).orElse("");
        String modelResourceOption = modelResource.map(MapServiceOptions::getModelResourceOption).orElse("");
        String tenantIdOption = tenantId.map(MapServiceOptions::getTenantIdOption).orElse("");
        String locationSetNameOption = locationSetName.map(MapServiceOptions::getLocationSetNameOption).orElse("");
        String maxDistanceFromRoadOption = maxDistanceFromRoad.map(MapServiceOptions::getMaxDistanceFromRoadOption).orElse("");
        String transportTypeOption = transportType == null
                ? ""
                : MapServiceOptions.getTransportTypeOption(transportType.value());
        String options = Stream
                .of(providerOption, locationOption, modelOption, modelVersionOption, modelResourceOption, tenantIdOption,
                        locationSetNameOption,
                        maxDistanceFromRoadOption, transportTypeOption)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return options.isEmpty() ? "" : options;
    }

    private static List<TransportType> resolveTransportTypes(Optional<String> transportType) {
        return transportType
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .map(TransportType::of)
                        .distinct()
                        .toList())
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> List.of(TransportType.CAR));
    }

}
