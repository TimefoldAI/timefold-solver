package ai.timefold.solver.model.maps.haversine.impl;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.integration.internal.provider.WaypointsProvider;

@ApplicationScoped
public class HaversineWaypointsProvider extends HaversineProviderIdentification implements WaypointsProvider {

    @Override
    public List<Location> getWaypoints(List<Location> locations, Map<String, String> options) {
        return locations;
    }
}
