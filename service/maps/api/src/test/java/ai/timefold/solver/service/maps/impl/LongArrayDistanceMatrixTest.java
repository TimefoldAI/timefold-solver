package ai.timefold.solver.service.maps.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

import ai.timefold.solver.service.maps.api.IndexableDistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;

import org.junit.jupiter.api.Test;

public class LongArrayDistanceMatrixTest {
    private static final int[][] expectedIndices = {
            new int[] { 0, 2, 5, 9, 14, 20, 27, 35 },
            new int[] { 1, 4, 8, 13, 19, 26, 34 },
            new int[] { 3, 7, 12, 18, 25, 33 },
            new int[] { 6, 11, 17, 24, 32 },
            new int[] { 10, 16, 23, 31 },
            new int[] { 15, 22, 30 },
            new int[] { 21, 29 },
            new int[] { 28 },
    };

    @Test
    void getIndex() {
        for (int from = 0; from < expectedIndices.length; from++) {
            for (int to = from; to < expectedIndices[from].length; to++) {
                assertThat(LongArrayDistanceMatrix.getIndex(from, to))
                        .withFailMessage("Index (" + from + ", " + to + ") differs from expected value ("
                                + expectedIndices[from][to - from] + ") but was instead ("
                                + LongArrayDistanceMatrix.getIndex(from, to) + ").")
                        .isEqualTo(expectedIndices[from][to - from]);
            }
        }
    }

    @Test
    void cacheShortIndexInLocation() {
        LongArrayDistanceMatrix distanceMatrix = new LongArrayDistanceMatrix(2);
        Location a = new Location(0.0, 1.0);
        Location b = new Location(1.0, 0.0);

        assertThat(a.getIndex(distanceMatrix)).isEqualTo(IndexableDistanceMatrix.EMPTY_INDEX);
        assertThat(b.getIndex(distanceMatrix)).isEqualTo(IndexableDistanceMatrix.EMPTY_INDEX);

        a.setDistanceMatrix(distanceMatrix);
        b.setDistanceMatrix(distanceMatrix);

        assertThat((int) a.getIndex(distanceMatrix)).isEqualTo(0);
        assertThat((int) b.getIndex(distanceMatrix)).isEqualTo(1);

        long distance = 0L;
        for (Location from : List.of(a, b)) {
            for (Location to : List.of(a, b)) {
                distanceMatrix.put(from, to, distance);
                distance++;
            }
        }

        assertThat((int) a.getIndex(distanceMatrix)).isEqualTo(0);
        assertThat((int) b.getIndex(distanceMatrix)).isEqualTo(1);
    }

    @Test
    void putNoResize() {
        LongArrayDistanceMatrix distanceMatrix = new LongArrayDistanceMatrix(4);
        Location a = new Location(0.0, 1.0);
        Location b = new Location(1.0, 0.0);
        Location c = new Location(0.25, 0.75);
        Location d = new Location(0.75, 0.5);

        long distance = 0L;
        for (Location from : List.of(a, b, c, d)) {
            for (Location to : List.of(a, b, c, d)) {
                distanceMatrix.put(from, to, distance);
                distance++;
            }
        }

        long expectedDistance = 0L;
        int firstIndex = 0;
        for (Location from : List.of(a, b, c, d)) {
            int secondIndex = 0;
            for (Location to : List.of(a, b, c, d)) {
                assertThat(distanceMatrix.get(from, to))
                        .withFailMessage("Incorrect get value for index (" + firstIndex + ", " + secondIndex + ")")
                        .isEqualTo(expectedDistance);
                expectedDistance++;
                secondIndex++;
            }
            firstIndex++;
        }
    }

    @Test
    void putWithResize() {
        LongArrayDistanceMatrix distanceMatrix = new LongArrayDistanceMatrix(2);
        Location a = new Location(0.0, 1.0);
        Location b = new Location(1.0, 0.0);
        Location c = new Location(0.25, 0.75);
        Location d = new Location(0.75, 0.5);

        long distance = 0L;
        for (Location from : List.of(a, b, c, d)) {
            for (Location to : List.of(a, b, c, d)) {
                distanceMatrix.put(from, to, distance);
                distance++;
            }
        }

        long expectedDistance = 0L;
        int firstIndex = 0;
        for (Location from : List.of(a, b, c, d)) {
            int secondIndex = 0;
            for (Location to : List.of(a, b, c, d)) {
                assertThat(distanceMatrix.get(from, to))
                        .withFailMessage("Incorrect get value for index (" + firstIndex + ", " + secondIndex + ")")
                        .isEqualTo(expectedDistance);
                expectedDistance++;
                secondIndex++;
            }
            firstIndex++;
        }
    }

    @Test
    void valuesPreservedAfterResize() {
        LongArrayDistanceMatrix distanceMatrix = new LongArrayDistanceMatrix(2);
        Location a = new Location(0.0, 1.0);
        Location b = new Location(1.0, 0.0);
        Location c = new Location(0.25, 0.75);
        Location d = new Location(0.75, 0.5);

        long distance = 0L;
        for (Location from : List.of(a, b, c, d)) {
            for (Location to : List.of(a, b, c, d)) {
                distanceMatrix.put(from, to, distance);
                distance++;
            }
        }

        distanceMatrix.resize(24);

        long expectedDistance = 0L;
        int firstIndex = 0;
        for (Location from : List.of(a, b, c, d)) {
            int secondIndex = 0;
            for (Location to : List.of(a, b, c, d)) {
                assertThat(distanceMatrix.get(from, to))
                        .withFailMessage("Incorrect get value for index (" + firstIndex + ", " + secondIndex + ")")
                        .isEqualTo(expectedDistance);
                expectedDistance++;
                secondIndex++;
            }
            firstIndex++;
        }
    }

    @Test
    void manyPutWithResize() {
        DoubleFunction<Location> locationGenerator = (id) -> new Location(id, id);
        LongArrayDistanceMatrix distanceMatrix = new LongArrayDistanceMatrix(2);
        List<Location> locationList = new ArrayList<>(1000);
        for (int i = -900; i < 900; i++) {
            locationList.add(locationGenerator.apply(i * 0.1));
        }

        long distance = 0L;
        for (Location from : locationList) {
            for (Location to : locationList) {
                distanceMatrix.put(from, to, distance);
                distance++;
            }
        }

        long expectedDistance = 0L;
        int firstIndex = 0;
        for (Location from : locationList) {
            int secondIndex = 0;
            for (Location to : locationList) {
                assertThat(distanceMatrix.get(from, to))
                        .withFailMessage("Incorrect get value for index (" + firstIndex + ", " + secondIndex + ")")
                        .isEqualTo(expectedDistance);
                expectedDistance++;
                secondIndex++;
            }
            firstIndex++;
        }
    }

    @Test
    void tooLargeExpectedSizeFailFast() {
        assertThatCode(() -> new LongArrayDistanceMatrix(LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedLocationCount (" + (LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + 1)
                        + ") is too large; maximum expectedLocationCount is "
                        + LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + ".");
    }

    @Test
    void tooSmallExpectedSizeFailFast() {
        assertThatCode(() -> new LongArrayDistanceMatrix(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedLocationCount (" + -1
                        + ") is too small; minimum expectedLocationCount is 0.");
    }

    @Test
    void tooLargeResizeFailFast() {
        assertThatCode(() -> new LongArrayDistanceMatrix(0).resize(LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedLocationCount (" + (LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + 1)
                        + ") is too large; maximum expectedLocationCount is "
                        + LongArrayDistanceMatrix.MAXIMUM_LOCATION_COUNT + ".");
    }

    @Test
    void tooSmallResizeFailFast() {
        assertThatCode(() -> new LongArrayDistanceMatrix(0).resize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedLocationCount (" + -1
                        + ") is too small; minimum expectedLocationCount is 0.");
    }

    @Test
    void invalidLatitudeShouldFail() {
        assertThatCode(() -> new Location(-91, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Latitude must be between -90 and 90, found -91");
    }

    @Test
    void invalidLongitudeShouldFail() {
        assertThatCode(() -> new Location(1, -181))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Longitude must be between -180 and 180, found -181");
    }

}
