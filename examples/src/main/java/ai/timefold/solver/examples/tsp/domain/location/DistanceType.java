package ai.timefold.solver.examples.tsp.domain.location;

public enum DistanceType {
    /**
     * Requires that all {@link Location} instances are of type {@link AirLocation}.
     */
    AIR_DISTANCE(AirLocation::new),
    /**
     * Requires that all {@link Location} instances are of type {@link RoadLocation}.
     */
    ROAD_DISTANCE(RoadLocation::new),
    /**
     * Requires that all {@link Location} instances are of type {@link GeoLocation}.
     */
    GEO(GeoLocation::new);

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
