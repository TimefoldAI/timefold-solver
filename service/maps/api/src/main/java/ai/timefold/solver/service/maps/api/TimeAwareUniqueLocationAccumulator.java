package ai.timefold.solver.service.maps.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;

/**
 * Accumulates {@link Location}s with their time availability data while checking for duplicates.
 */
public final class TimeAwareUniqueLocationAccumulator {
    private final Set<Location> instanceUniqueLocations = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Location> equalityUniqueLocations = new LinkedHashSet<>();
    private final Map<Location, List<TimeInterval>> locationsWithAvailability = new LinkedHashMap<>();

    /**
     * Adds a single {@link Location} while checking for duplicates.
     * <p>
     * For each unique location, the time intervals when the location is available for traveling are merged.
     *
     * @return true if the accumulator already contains a different equal instance of this location, otherwise false
     */
    public boolean addLocation(Location location, List<TimeInterval> availableTimes) {
        boolean addedEquality = equalityUniqueLocations.add(location);
        boolean addedInstance = instanceUniqueLocations.add(location);
        locationsWithAvailability.computeIfAbsent(location, l -> new ArrayList<>()).addAll(availableTimes);
        // There are two equal instances, which means that locations have not been deduplicated.
        return !addedEquality && addedInstance;
    }

    /**
     * Retrieves a map of locations and their corresponding availability time intervals.
     * Each location is associated with a list of time intervals during which it is available for traveling.
     *
     * @return a map where the keys are {@link Location} instances and the values are lists of {@link TimeInterval} instances
     *         representing the periods during which the locations are available.
     */
    public Map<Location, List<TimeInterval>> getLocationsWithAvailability() {
        return locationsWithAvailability;
    }
}
