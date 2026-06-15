package ai.timefold.solver.service.maps.service.client.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.ToIntFunction;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;

/**
 * Return type for the availability-based travel time and distance fetch. Holds per-timeframe travel-time and distance
 * matrices as plain arrays in a fixed order defined by the client's bucketing, plus a
 * {@code ToIntFunction<OffsetDateTime>} that resolves an instant to an array index. Callers (typically the enricher)
 * hand the arrays and the resolver to {@link Location} without ever seeing the bucketing details themselves. The
 * solver-facing API is pure {@link OffsetDateTime} in, array index out.
 * <p>
 * {@code locationsNotInMap} is the union of locations that fell out of the map across every timeframe call; the list
 * preserves insertion order of the caller's original location list and contains no duplicates.
 */
public record TravelTimesByAvailabilityWithMetadata(DistanceMatrix[] travelTimesByTimeframe,
        DistanceMatrix[] distancesByTimeframe,
        List<Location> locationsNotInMap,
        ToIntFunction<OffsetDateTime> timeframeIndexResolver) {
}
