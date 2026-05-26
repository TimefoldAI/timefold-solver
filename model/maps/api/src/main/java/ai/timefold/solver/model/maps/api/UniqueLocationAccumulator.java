package ai.timefold.solver.model.maps.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.model.maps.api.model.Location;

/**
 * Accumulates {@link Location}s while checking for duplicates.
 */
public final class UniqueLocationAccumulator {
    private final Set<Location> instanceUniqueLocations = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Location> equalityUniqueLocations = new LinkedHashSet<>();

    /**
     * Adds a single {@link Location} while checking for duplicates.
     *
     * @return true if the accumulator already contains a different equal instance of this location, otherwise false
     */
    public boolean addLocation(Location location) {
        boolean addedEquality = equalityUniqueLocations.add(location);
        boolean addedInstance = instanceUniqueLocations.add(location);
        // There are two equal instances, which means that locations have not been deduplicated.
        return !addedEquality && addedInstance;
    }

    /**
     * Retrieves a {@link List} of unique {@link Location}s.
     */
    public List<Location> asList() {
        return new ArrayList<>(equalityUniqueLocations);
    }
}
