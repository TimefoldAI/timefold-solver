package ai.timefold.solver.model.definition.api.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record MapsConfiguration(String provider, String location, Double maxDistanceFromRoad, String transportType,
        Boolean useTraffic) {

    public MapsConfiguration(String provider) {
        this(provider, null, null, null, null);
    }

    public MapsConfiguration withLocation(String location) {
        return new MapsConfiguration(provider, location, maxDistanceFromRoad, transportType, useTraffic);
    }

    public MapsConfiguration withMaxDistanceFromRoad(Double maxDistanceFromRoad) {
        return new MapsConfiguration(provider, location, maxDistanceFromRoad, transportType, useTraffic);
    }

    public MapsConfiguration withTransportType(String transportType) {
        return new MapsConfiguration(provider, location, maxDistanceFromRoad, transportType, useTraffic);
    }

    public MapsConfiguration withUseTraffic(Boolean useTraffic) {
        return new MapsConfiguration(provider, location, maxDistanceFromRoad, transportType, useTraffic);
    }

    public MapsConfiguration override(MapsConfiguration configuration) {
        String finalLocation = location;
        String finalProvider = provider;
        Double finalMaxDistanceFromRoad = maxDistanceFromRoad;
        String finalTransportType = transportType;
        Boolean finalUseTraffic = useTraffic;

        if (configuration == null) {
            return this;
        }

        if (location == null) {
            finalLocation = configuration.location();
        }
        if (provider == null) {
            finalProvider = configuration.provider();
        }
        if (maxDistanceFromRoad == null) {
            finalMaxDistanceFromRoad = configuration.maxDistanceFromRoad();
        }
        if (transportType == null) {
            finalTransportType = configuration.transportType();
        }
        if (useTraffic == null) {
            finalUseTraffic = configuration.useTraffic();
        }

        return new MapsConfiguration(finalProvider, finalLocation, finalMaxDistanceFromRoad, finalTransportType,
                finalUseTraffic);
    }

}
