package ai.timefold.solver.examples.tsp.domain.location;

public enum DistanceType {
    AIR_DISTANCE(AirLocation::new),
    ROAD_DISTANCE(RoadLocation::new),
    GEOGRAPHIC_DISTANCE(GeoLocation::new),
    PSEUDO_ROAD_DISTANCE(PseudoRoadLocation::new); // Niche distance type for TSPLIB95.

    private final LocationFunction locationFunction;

    DistanceType(LocationFunction locationFunction) {
        this.locationFunction = locationFunction;
    }

    public <Location_ extends Location> Location_ createLocation(long id, double x, double y) {
        return (Location_) locationFunction.apply(id, x, y);
    }

    interface LocationFunction {
        Location apply(long id, double x, double y);
    }

}
