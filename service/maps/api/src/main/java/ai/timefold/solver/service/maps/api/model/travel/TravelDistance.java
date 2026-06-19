package ai.timefold.solver.service.maps.api.model.travel;

import ai.timefold.solver.service.maps.api.model.Location;

import org.jspecify.annotations.NullMarked;

/**
 * Represents the travel distance of a potentially unreachable route between two
 * {@link Location}-s.
 *
 * @param meters The travel distance in meters. Important: the value is undefined when {@link #isReachable()} returns false
 *        and an attempt to retrieve it will throw an {@link IllegalStateException}.
 */
@NullMarked
public record TravelDistance(long meters) {

    // the representation of unreachable travel distance must not leak through the API
    private static final long UNREACHABLE_TRAVEL_DISTANCE = Long.MAX_VALUE;

    public static final TravelDistance ZERO = new TravelDistance(0);
    public static final TravelDistance UNREACHABLE = new TravelDistance(UNREACHABLE_TRAVEL_DISTANCE);

    /**
     * Returns a new or shared instance of reachable travel distance representing the given number of meters.
     *
     * @param meters the number of meters
     * @return a new or shared instance of {@link TravelDistance}
     */
    public static TravelDistance of(long meters) {
        return meters == 0L ? ZERO : new TravelDistance(meters);
    }

    /**
     * Returns true if this travel distance represents the distance of a reachable route.
     *
     * @return true If this travel distance represents the distance of a reachable route, false otherwise.
     */
    public boolean isReachable() {
        return meters < UNREACHABLE_TRAVEL_DISTANCE;
    }

    /**
     * Returns the travel distance in meters. If the travel distance represents an unreachable travel route distance,
     * throws {@link IllegalStateException}.
     *
     * @return the number of meters
     * @throws IllegalStateException when the travel distance is not reachable
     */
    public long meters() {
        if (!isReachable()) {
            throw new IllegalStateException("Cannot retrieve an unreachable TravelDistance value.");
        }
        return meters;
    }

    /**
     * Returns the travel distance in seconds, if reachable. If not, returns zero.
     *
     * @return the number of reachable meters
     */
    public long reachableMeters() {
        return isReachable() ? meters : 0L;
    }

}
