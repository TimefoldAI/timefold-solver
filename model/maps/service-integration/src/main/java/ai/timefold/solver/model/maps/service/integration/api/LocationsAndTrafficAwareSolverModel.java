package ai.timefold.solver.model.maps.service.integration.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.TimeInterval;

/**
 * A solver model whose locations need travel-time matrices that vary by time of day. Exposes the set of locations and,
 * for each of them, the time intervals during which travel may occur. The enricher uses this map to decide which
 * timeframes (buckets of time) to fetch from the maps service.
 */
public interface LocationsAndTrafficAwareSolverModel<Score_ extends Score<Score_>>
        extends LocationsAwareSolverModel<Score_> {

    /**
     * @return the locations relevant to the plan, mapped to the time intervals during which each location may be
     *         involved in travel. Each {@link TimeInterval} covers a continuous range {@code [from, to]}; the enricher
     *         determines which timeframe buckets the interval overlaps and fetches only the matrices needed for those
     *         buckets. A location relevant to only one timeframe needs a single narrow interval; a location active
     *         across the whole day would have a wider interval (or multiple intervals). Must not be {@code null} or
     *         empty during enrichment. Every key must also appear in {@link #getLocations()}.
     */
    Map<Location, List<TimeInterval>> getLocationsWithTimeAvailability();

    /**
     * Location sets are not supported on the traffic-aware path.
     */
    @Override
    default Optional<String> getLocationSetName() {
        return Optional.empty();
    }

}
