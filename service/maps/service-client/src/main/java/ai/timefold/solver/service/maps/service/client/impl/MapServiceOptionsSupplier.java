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

    private final TransportType transportType;

    private final List<TransportType> allowedTransportTypes;

    public MapServiceOptionsSupplier(
            @ConfigProperty(name = "timefold.platform.map-service.provider") Optional<String> provider,
            @ConfigProperty(name = "timefold.platform.map-service.location") Optional<String> location,
            @ConfigProperty(
                    name = "timefold.platform.map-service.max-distance-from-road") Optional<Double> maxDistanceFromRoad,
            @ConfigProperty(name = "timefold.model.id") Optional<String> model,
            @ConfigProperty(name = "timefold.model.api-version") Optional<String> modelVersion,
            @ConfigProperty(name = "timefold.model.rest-resource") Optional<String> modelResource,
            @ConfigProperty(name = "timefold.platform.tenant-id") Optional<String> tenantId,
            @ConfigProperty(name = "timefold.platform.map-service.transport-type") Optional<String> transportType,
            @ConfigProperty(
                    name = "timefold.platform.map-service.allowed-transport-types") Optional<String> allowedTransportTypes) {
        this.provider = provider;
        this.location = location;
        this.model = model;
        this.modelVersion = modelVersion;
        this.modelResource = modelResource;
        this.tenantId = tenantId;
        this.maxDistanceFromRoad = maxDistanceFromRoad;
        this.transportType = resolveTransportType(transportType);
        this.allowedTransportTypes = resolveAllowedTransportTypes(allowedTransportTypes);
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public boolean isAutoSelectTransportType() {
        return transportType.isAutoSelect();
    }

    public List<TransportType> getAllowedTransportTypes() {
        return allowedTransportTypes;
    }

    public boolean isAllowed(TransportType transportType) {
        if (transportType == null || transportType.isAutoSelect()) {
            return false;
        }
        return allowedTransportTypes.isEmpty() || allowedTransportTypes.contains(transportType);
    }

    public TransportType getDefaultTransportType() {
        if (!isAutoSelectTransportType()) {
            return transportType;
        }
        if (isAllowed(TransportType.defaultType())) {
            return TransportType.defaultType();
        }
        return allowedTransportTypes.getFirst();
    }

    public String getOptions() {
        return options().build();
    }

    public OptionsBuilder options() {
        return new OptionsBuilder();
    }

    private static TransportType resolveTransportType(Optional<String> transportType) {
        return transportType
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(TransportType::of)
                .orElse(TransportType.defaultType());
    }

    private static List<TransportType> resolveAllowedTransportTypes(Optional<String> allowedTransportTypes) {
        List<TransportType> allowed = allowedTransportTypes
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .map(TransportType::of)
                        .distinct()
                        .toList())
                .orElseGet(List::of);
        if (allowed.stream().anyMatch(TransportType::isAutoSelect)) {
            throw new IllegalArgumentException(
                    "The transport type (%s) cannot be part of the allowed transport types (%s); it is not a routing profile."
                            .formatted(TransportType.AUTO_SELECT, allowed));
        }
        return allowed;
    }

    public final class OptionsBuilder {

        private String locationSetName;

        private TransportType transportType = getDefaultTransportType();

        private OptionsBuilder() {
        }

        public OptionsBuilder locationSetName(String locationSetName) {
            this.locationSetName = locationSetName;
            return this;
        }

        public OptionsBuilder transportType(TransportType transportType) {
            this.transportType = transportType;
            return this;
        }

        public String build() {
            String providerOption = provider.map(MapServiceOptions::getProviderOption).orElse("");
            String locationOption = location.map(MapServiceOptions::getLocationOption).orElse("");
            String modelOption = model.map(MapServiceOptions::getModelOption).orElse("");
            String modelVersionOption = modelVersion.map(MapServiceOptions::getModelVersionOption).orElse("");
            String modelResourceOption = modelResource.map(MapServiceOptions::getModelResourceOption).orElse("");
            String tenantIdOption = tenantId.map(MapServiceOptions::getTenantIdOption).orElse("");
            String locationSetNameOption =
                    locationSetName == null ? "" : MapServiceOptions.getLocationSetNameOption(locationSetName);
            String maxDistanceFromRoadOption =
                    maxDistanceFromRoad.map(MapServiceOptions::getMaxDistanceFromRoadOption).orElse("");
            String transportTypeOption = transportType == null || transportType.isAutoSelect()
                    ? ""
                    : MapServiceOptions.getTransportTypeOption(transportType.value());
            return Stream
                    .of(providerOption, locationOption, modelOption, modelVersionOption, modelResourceOption,
                            tenantIdOption, locationSetNameOption, maxDistanceFromRoadOption, transportTypeOption)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }
    }
}
