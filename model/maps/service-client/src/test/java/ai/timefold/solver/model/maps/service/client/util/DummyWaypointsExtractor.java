package ai.timefold.solver.model.maps.service.client.util;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;

import ai.timefold.solver.model.maps.api.model.Waypoints;
import ai.timefold.solver.model.maps.service.integration.api.WaypointsExtractor;

@Dependent
public class DummyWaypointsExtractor implements WaypointsExtractor<SampleModel> {

    @Override
    public List<Waypoints> extractBaseWaypoints(SampleModel userModel) {

        int index = 0;
        int size = userModel.getLocations().size();
        int pairSize = size / 2;
        List<Waypoints> waypoints = new ArrayList<>();

        if (pairSize > 0) {
            for (int i = 0; i < pairSize; i++) {
                waypoints.add(new Waypoints("id_" + i,
                        List.of(userModel.getLocations().get(index++), userModel.getLocations().get(index++))));
            }
        } else {
            waypoints.add(new Waypoints("id", userModel.getLocations()));
        }

        return waypoints;
    }
}
