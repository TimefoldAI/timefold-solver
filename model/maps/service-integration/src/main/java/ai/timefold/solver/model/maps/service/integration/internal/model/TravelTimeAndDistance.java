package ai.timefold.solver.model.maps.service.integration.internal.model;

import java.util.Objects;

import ai.timefold.solver.model.maps.api.DistanceMatrix;

public record TravelTimeAndDistance(DistanceMatrix travelTime, DistanceMatrix distance) {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TravelTimeAndDistance that = (TravelTimeAndDistance) o;
        return Objects.equals(travelTime, that.travelTime) && Objects.equals(distance, that.distance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(travelTime, distance);
    }
}
