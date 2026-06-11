package ai.timefold.solver.service.definition.api.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record MapsConfiguration(String provider, String location, Double maxDistanceFromRoad, String transportType) {

    public MapsConfiguration override(MapsConfiguration configuration) {
        String finalLocation = location;
        String finalProvider = provider;
        Double finalMaxDistanceFromRoad = maxDistanceFromRoad;
        String finalTransportType = transportType;

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

        return new MapsConfiguration(finalProvider, finalLocation, finalMaxDistanceFromRoad, finalTransportType);
    }

}
