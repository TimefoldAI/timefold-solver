package ai.timefold.solver.examples.tsp.domain.location;

import java.util.Map;

import ai.timefold.solver.examples.common.persistence.jackson.KeySerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The cost between 2 locations was precalculated on a real road network route.
 * The cost itself might be the distance in km, the travel time, the fuel usage or a weighted function of any of those.
 * Used with {@link DistanceType#ROAD_DISTANCE}.
 */
public class RoadLocation extends Location {

    // Prefer Map over array or List because customers might be added and removed in real-time planning.
    protected Map<RoadLocation, Double> travelDistanceMap;

    public RoadLocation() {
    }

    public RoadLocation(long id) {
        super(id);
    }

    public RoadLocation(long id, double latitude, double longitude) {
        super(id, latitude, longitude);
    }

    @JsonSerialize(keyUsing = KeySerializer.class)
    @JsonDeserialize(keyUsing = RoadLocationKeyDeserializer.class)
    public Map<RoadLocation, Double> getTravelDistanceMap() {
        return travelDistanceMap;
    }

    public void setTravelDistanceMap(Map<RoadLocation, Double> travelDistanceMap) {
        this.travelDistanceMap = travelDistanceMap;
    }

    @Override
    public long getDistanceTo(Location location) {
        if (this == location) {
            return 0L;
        }
        Double distance = travelDistanceMap.get((RoadLocation) location);
        if (distance == null) {
            throw new IllegalStateException("The travelDistanceMap in location (%s) does not contain the key (%s)."
                    .formatted(this, location));
        }
        return adjust(distance);
    }

}
