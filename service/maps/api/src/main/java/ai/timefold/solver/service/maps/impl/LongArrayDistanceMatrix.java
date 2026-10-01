package ai.timefold.solver.service.maps.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.service.maps.api.IndexableDistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A matrix that stores locations in a single long array.
 * The distance at index x corresponds to the item with x
 * in the below matrix:
 *
 * <pre>
 * 0 2 5 9
 * 1 4 8
 * 3 7
 * 6
 * </pre>
 *
 * This arrangement means all entries correspond to a location with x as the id
 * only occur after the index (x * x) in the array. This means the array can be
 * resized without moving elements. It can store the distance matrix corresponding
 * to a maximum of {@link #MAXIMUM_LOCATION_COUNT} locations.
 *
 */
public final class LongArrayDistanceMatrix implements IndexableDistanceMatrix {
    /**
     * The maximum number of locations the matrix can store.
     * {@code 46341 * 46341} overflows a signed int, so
     * it must have less than {@code 46340} elements (because
     * the array size is {@code (count + 1)*(count + 1)}, not
     * {@code count * count}, since items on the diagonal (i.e. (i, i))
     * takes two entries (and ignores one of them)
     * instead of one to simplify calculations).
     */
    final static int MAXIMUM_LOCATION_COUNT = 46339;

    /**
     * Stores distance between two pair of locations. See
     * {@link LongArrayDistanceMatrix} JavaDoc for information
     * on how the indices are arranged.
     */
    long[] distanceBuckets;

    @JsonDeserialize(keyUsing = LocationKeyDeserializer.class)
    final Map<Location, Short> locationToId = new HashMap<>();
    final Set<Short> originLocationsID = new HashSet<>();
    final Set<Short> destLocationsID = new HashSet<>();
    short idGenerator = 0;

    private static void assertValidSize(int expectedLocationCount) {
        if (expectedLocationCount < 0) {
            throw new IllegalArgumentException(
                    "expectedLocationCount (" + expectedLocationCount + ") is too small; minimum expectedLocationCount is 0.");
        }
        if (expectedLocationCount > MAXIMUM_LOCATION_COUNT) {
            throw new IllegalArgumentException("expectedLocationCount (" + expectedLocationCount
                    + ") is too large; maximum expectedLocationCount is " + MAXIMUM_LOCATION_COUNT + ".");
        }
    }

    public LongArrayDistanceMatrix(int expectedLocationCount) {
        assertValidSize(expectedLocationCount);
        int arraySize = expectedLocationCount + 1;
        distanceBuckets = new long[arraySize * arraySize];
    }

    public LongArrayDistanceMatrix() {

    }

    public long[] getDistanceBuckets() {
        return distanceBuckets;
    }

    /**
     * Resizes the distance matrix, so it can hold entries corresponding
     * to every pair in a collection of a given size.
     *
     * @param expectedLocationCount The expected size of the collection.
     */
    void resize(int expectedLocationCount) {
        assertValidSize(expectedLocationCount);
        int arraySize = expectedLocationCount + 1;
        distanceBuckets = Arrays.copyOf(distanceBuckets, arraySize * arraySize);
    }

    /**
     * Triangular numbers are 0, 1, 3, 6, 10, ...
     * In particular, triangular[i+1] = triangular[i] + (i+1).
     * Also known as the sum of all integers up to a given index. (i.e. 0, 0 + 1, 0 + 1 + 2, ...)
     * See <a href="https://oeis.org/A000217">OEIS entry for the formula of triangular numbers</a>.
     */
    private static int getTriangularNumber(int index) {
        return (index * (index + 1)) >> 1;
    }

    /**
     * Return the single array index corresponding to pair of array indices [fromLocation, toLocation],
     * where fromLocation &lt; toLocation.
     * <br/>
     * Note: Rows start at 0, 1, 3, 6, 10 (i.e. the triangular numbers),
     * since in the diagonal starting at the first entry of a given row, there will be
     * exactly row index elements in that diagonal (i.e. 1 element in the first diagonal,
     * 2 elements in the second diagonal, 3 elements in third diagonal, etc.).
     * <br/>
     * Columns can be derived by observing each column increment
     * by a triangular number:
     *
     * <pre>
     * column 0 starts at 0 and increments by 1, 2, 3, 4
     * column 1 starts at 2 and increments by 2, 3, 4, 5
     * column 2 starts at 5 and increments by 3, 4, 5, 6
     * </pre>
     */
    public static int getIndex(int fromLocation, int toLocation) {
        int columnOffset =
                getTriangularNumber(toLocation + 2) - getTriangularNumber(fromLocation + 2) - (toLocation - fromLocation);
        return getTriangularNumber(fromLocation) + columnOffset;
    }

    /**
     * Calculates what index the distance corresponding to the pair (fromLocation, toLocation)
     * is stored in {@link #distanceBuckets}. First, the index corresponding to
     * [min(fromLocation.id, toLocation.id), max(fromLocation.id, toLocation.id)] is looked up.
     * If fromLocation.id &le toLocation.id, then it is doubled. If toLocation.id &gt fromLocation.id,
     * it is doubled and 1 is added to it.
     */
    private int getIndex(Location fromLocation, Location toLocation) {
        short fromLocationShortId = getShortId(fromLocation);
        short toLocationShortId = getShortId(toLocation);

        // conversion to unsigned int makes short sufficient for addressing MAXIMUM_LOCATION_COUNT locations
        int fromLocationId = Short.toUnsignedInt(fromLocationShortId);
        int toLocationId = Short.toUnsignedInt(toLocationShortId);
        int offset = 0;
        if (fromLocationId > toLocationId) {
            offset = 1;
            int temp = fromLocationId;
            fromLocationId = toLocationId;
            toLocationId = temp;
        }
        return 2 * getIndex(fromLocationId, toLocationId) + offset;
    }

    private short getShortId(Location location) {
        // to support multi-thread solving, it is expected that the location index cache is pre-populated
        // by a single thread before multiple threads start accessing it, therefore no synchronization is done here
        // to avoid performance penalty
        short cachedShortId = location.getIndex(this);
        if (cachedShortId != EMPTY_INDEX) {
            return cachedShortId;
        }
        Short shortId = locationToId.get(location);
        if (shortId == null) {
            shortId = idGenerator;
            locationToId.put(location, shortId);
            location.setIndex(this, shortId);
            idGenerator++;
        }
        return shortId;
    }

    /**
     * Updates the cached index for this distance matrix in given {@link Location}. If the cached index is already set,
     * keeps its current value.
     * <p>
     * In order to support multi-thread solving, it is expected that the location cached index is pre-populated
     * by a single thread before multiple threads start accessing it.
     *
     * @param location The location instance whose cached index should be updated, if needed.
     */
    @Override
    public void updateCachedIndex(Location location) {
        getShortId(location);
    }

    @Override
    public long get(Location from, Location to) {
        int index = getIndex(from, to);
        if (index > distanceBuckets.length) {
            return -1L;
        }
        return distanceBuckets[index];
    }

    /**
     * Put a new distance inside {@link #distanceBuckets}, increasing the matrix size if
     * necessary. Both locations must have an id set. Order is significant.
     */
    @Override
    public void put(Location from, Location to, long distance) {
        int index = getIndex(from, to);
        if (index > distanceBuckets.length) {
            // Note: this is space efficient, but not time efficient if
            // locations are frequently added.
            resize(Math.max(
                    Short.toUnsignedInt(locationToId.get(from)),
                    Short.toUnsignedInt(locationToId.get(to))) + 1);
        }
        originLocationsID.add(getShortId(from));
        destLocationsID.add(getShortId(to));
        distanceBuckets[index] = distance;
    }

    @Override
    public int getNumberOfOriginLocations() {
        return originLocationsID.size();
    }

    @Override
    public int getNumberOfDestinationLocations() {
        return destLocationsID.size();
    }

    // Methods needed for deserialization

    public Set<Short> getOriginLocationsID() {
        return originLocationsID;
    }

    public Set<Short> getDestinationLocationsID() {
        return destLocationsID;
    }

    public Map<Location, Short> getLocationToId() {
        return locationToId;
    }
}
