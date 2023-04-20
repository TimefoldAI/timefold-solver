package ai.timefold.solver.examples.tsp.persistence;

import java.io.File;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.examples.tsp.domain.location.DistanceType;
import ai.timefold.solver.examples.tsp.domain.location.RoadLocation;

public final class TspSolutionFileIO extends AbstractJsonSolutionFileIO<TspSolution> {

    public TspSolutionFileIO() {
        super(TspSolution.class);
    }

    @Override
    public TspSolution read(File inputSolutionFile) {
        TspSolution tspSolution = super.read(inputSolutionFile);

        if (tspSolution.getDistanceType() == DistanceType.ROAD_DISTANCE) {
            deduplicateRoadLocations(tspSolution);
        }

        return tspSolution;
    }

    private void deduplicateRoadLocations(TspSolution tspSolution) {
        var roadLocationList = tspSolution.getLocationList().stream()
                .filter(location -> location instanceof RoadLocation)
                .map(location -> (RoadLocation) location)
                .collect(Collectors.toList());
        var locationsById = roadLocationList.stream()
                .collect(Collectors.toMap(RoadLocation::getId, Function.identity()));
        /*
         * Replace the duplicate RoadLocation instances in the travelDistanceMap by references to instances from
         * the locationList.
         */
        for (RoadLocation roadLocation : roadLocationList) {
            var newTravelDistanceMap = deduplicateMap(roadLocation.getTravelDistanceMap(),
                    locationsById, RoadLocation::getId);
            roadLocation.setTravelDistanceMap(newTravelDistanceMap);
        }
    }

}
