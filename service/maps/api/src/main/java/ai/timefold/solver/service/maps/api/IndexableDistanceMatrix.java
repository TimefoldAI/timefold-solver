package ai.timefold.solver.service.maps.api;

import ai.timefold.solver.service.maps.api.model.Location;

/**
 * Represents a specific {@link DistanceMatrix} that supports using a {@link Location} index cache to enhance performance.
 */
public interface IndexableDistanceMatrix extends DistanceMatrix {

    /**
     * Represents that no index has been calculated yet.
     */
    short EMPTY_INDEX = -1;

    /**
     * Updates the cached index for this distance matrix in given {@link Location}.
     * <p>
     * In order to support multi-thread solving, it may be expected that the location cached index is pre-populated
     * by a single thread before multiple threads start accessing it. Check the concrete implementation for details.
     *
     * @param location The location instance whose cached index should be updated, if needed.
     */
    void updateCachedIndex(Location location);
}
