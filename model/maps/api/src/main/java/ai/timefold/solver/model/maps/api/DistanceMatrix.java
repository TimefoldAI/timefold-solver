package ai.timefold.solver.model.maps.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.impl.LongArrayDistanceMatrix;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = LongArrayDistanceMatrix.class)
public interface DistanceMatrix {

    /**
     * Create a new {@link DistanceMatrix} using the default implementation.
     *
     * @param expectedLocationCount The number of expected locations.
     */
    static DistanceMatrix getInstance(int expectedLocationCount) {
        return new LongArrayDistanceMatrix(expectedLocationCount);
    }

    /**
     * Create a new {@link DistanceMatrix} using the given from-location list
     * and to-location distance maps.
     *
     * @param locationList The from-locations.
     * @param distanceMaps Maps the contain the distance to each to-location for
     *        a particular from-location. There is a distance map for each location
     *        in {@code locationList}. The first map corresponds to the first location
     *        in {@code locationList}, the second map corresponds to the second location
     *        in {@code locationList}, and so on. For instance, for the following distance
     *        matrix:
     *
     *        <pre>
     *           A B C<br/>
     *         A 0 1 2<br/>
     *         B 2 0 1<br/>
     *         C 1 3 0
     *        </pre>
     *
     *        Use the following call to construct the distance matrix:
     *        {@code DistanceMatrix.fromMaps(List.of(A,B,C),
     *     Map.of(A, 0, B, 1, C, 2),
     *     Map.of(A, 2, B, 0, C, 1),
     *     Map.of(A, 1, B, 3, C, 0)
     * )}
     */
    @SafeVarargs
    static DistanceMatrix fromMaps(List<Location> locationList,
            Map<Location, Long>... distanceMaps) {
        if (locationList.size() != distanceMaps.length) {
            throw new IllegalArgumentException("Expected locationList (" + locationList
                    + ") to have same size as distanceMaps (" + Arrays.toString(distanceMaps) + ").");
        }
        DistanceMatrix distanceMatrix = getInstance(distanceMaps.length);
        for (int i = 0; i < locationList.size(); i++) {
            Location fromLocation = locationList.get(i);
            Map<Location, Long> travelTimes = distanceMaps[i];
            if (travelTimes.get(fromLocation) != 0) {
                throw new IllegalArgumentException("Expected map for location (" + fromLocation + ") to be zero for ("
                        + fromLocation + ") but was (" + distanceMaps[i].get(fromLocation) + ").");
            }
            if (travelTimes.size() != locationList.size()) {
                throw new IllegalArgumentException("Expected map for location (" + fromLocation + ") to have size ("
                        + locationList.size() + ") but was (" + distanceMaps[i].size() + ").");
            }
            for (Map.Entry<Location, Long> mapEntry : travelTimes.entrySet()) {
                distanceMatrix.put(fromLocation, mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return distanceMatrix;
    }

    /**
     * Gets the distance between from and to. Returns -1
     * if no distance was recorded. Order is significant.
     */
    long get(Location from, Location to);

    /**
     * Records the distance between from and to, replacing the
     * prior recorded distance if there was one. Order is significant.
     */
    void put(Location from, Location to, long distance);

    /**
     * Get number of origin locations
     */
    @JsonIgnore
    int getNumberOfOriginLocations();

    /**
     * Get number of destination location
     */
    @JsonIgnore
    int getNumberOfDestinationLocations();
}
