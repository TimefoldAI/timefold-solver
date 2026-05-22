package ai.timefold.solver.model.maps.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ai.timefold.solver.model.maps.api.model.Location;

/**
 * Deduplicates {@link Location} instances to ensure that only one instance of a specific
 * location exists in memory.
 */
public final class LocationDeduplicator {

    private final Map<Location, Location> knownLocations = new LinkedHashMap<>();

    /**
     * Returns an already known {@link Location} instance that is equal to the given location. If such an instance
     * does not exist yet, the given location is added to the known locations and returned.
     *
     * @param location the location to deduplicate
     * @return the deduplicated location
     */
    public Location same(Location location) {
        return knownLocations.computeIfAbsent(location, Function.identity());
    }

    /**
     * Retrieves a {@link List} of all unique {@link Location}s which have been deduplicated so far.
     */
    public List<Location> asList() {
        return new ArrayList<>(knownLocations.values());
    }
}
