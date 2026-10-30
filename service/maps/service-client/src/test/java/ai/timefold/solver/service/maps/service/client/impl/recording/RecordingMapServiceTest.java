package ai.timefold.solver.service.maps.service.client.impl.recording;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.StaticDaypartBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class RecordingMapServiceTest {

    private static final String OPTIONS = "provider:osrm,location:us-georgia";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Location a = new Location(1.0, 2.0);
    private final Location b = new Location(3.0, 4.0);

    @TempDir
    Path tempDir;

    @Test
    void recordsMatrices() throws IOException {
        var result = new TravelTimeAndDistanceWithMetadata(
                new TravelTimeAndDistance(matrix(0, 10, 20, 0), matrix(0, 100, 200, 0)), List.of(1), "us-georgia");
        var service = recordingService(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return result;
            }
        });

        assertThat(service.getTravelTimeAndDistance(List.of(a, b), OPTIONS)).isSameAs(result);

        var recorded = mapper.readValue(singleFile("distances-default.json").toFile(),
                TravelTimeAndDistanceWithMetadata.class);
        assertThat(value(recorded.travelTimeAndDistance().travelTime(), a, b)).isEqualTo(10L);
        assertThat(value(recorded.travelTimeAndDistance().travelTime(), b, a)).isEqualTo(20L);
        assertThat(value(recorded.travelTimeAndDistance().distance(), a, b)).isEqualTo(100L);
        assertThat(recorded.locationsNotInMapIdx()).containsExactly(1);
        assertThat(recorded.resolvedMapLocation()).isEqualTo("us-georgia");
    }

    @Test
    void recordsMatricesByTimeframe() throws IOException {
        var service = recordingService(new StubMapService() {
            @Override
            public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                    String options) {
                return new TravelTimesByTimeframeWithMetadata(
                        new DistanceMatrix[] { matrix(0, 1, 2, 0), matrix(0, 3, 4, 0), matrix(0, 5, 6, 0) },
                        new DistanceMatrix[] { matrix(0, 7, 8, 0), matrix(0, 7, 8, 0), matrix(0, 7, 8, 0) },
                        List.of(b), time -> 0);
            }
        });

        service.getTravelTimeAndDistanceByTimeframe(List.of(a, b), OPTIONS);

        var recorded = mapper.readValue(singleFile("distances-by-timeframe-default.json").toFile(),
                RecordingMapService.TimeframeRecording.class);
        assertThat(recorded.timeframes()).containsExactly("morning", "afternoon", "night");
        assertThat(recorded.locationsNotInMap()).containsExactly(b);
        assertThat(recorded.travelTimes()).hasSize(3);
        assertThat(value(recorded.travelTimes().get(1), a, b)).isEqualTo(3L);
        assertThat(value(recorded.travelTimes().get(2), b, a)).isEqualTo(6L);
        assertThat(value(recorded.distances().get(2), b, a)).isEqualTo(8L);
    }

    @Test
    void omitsTimeframesForSingleNonTrafficMatrix() throws IOException {
        var service = recordingService(new StubMapService() {
            @Override
            public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                    String options) {
                return new TravelTimesByTimeframeWithMetadata(new DistanceMatrix[] { matrix(0, 1, 2, 0) },
                        new DistanceMatrix[] { matrix(0, 3, 4, 0) }, List.of(), time -> 0);
            }
        });

        service.getTravelTimeAndDistanceByTimeframe(List.of(a, b), OPTIONS);

        var recorded = mapper.readValue(singleFile("distances-by-timeframe-default.json").toFile(),
                RecordingMapService.TimeframeRecording.class);
        assertThat(recorded.timeframes()).isNull();
        assertThat(recorded.travelTimes()).hasSize(1);
    }

    @Test
    void recordsLocationsOutOfMap() throws IOException {
        var service = recordingService(new StubMapService() {
            @Override
            public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
                return List.of(0);
            }
        });

        assertThat(service.getLocationsOutOfMap(List.of(a, b), OPTIONS)).containsExactly(0);

        List<Integer> recorded = mapper.readValue(singleFile("locations-out-of-map-default.json").toFile(),
                new TypeReference<>() {
                });
        assertThat(recorded).containsExactly(0);
    }

    @Test
    void doesNotCacheMatrixIndexesOnTheModelLocations() {
        DistanceMatrix travelTime = matrix(0, 10, 20, 0);
        var service = recordingService(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(travelTime, travelTime), List.of());
            }
        });
        var modelA = new Location(1.0, 2.0);
        var modelB = new Location(3.0, 4.0);

        service.getTravelTimeAndDistance(List.of(modelA, modelB), OPTIONS);

        // The model attaches the matrix itself afterwards; it must see the same values as without recording.
        modelA.setTravelTimeMatrix(travelTime);
        modelB.setTravelTimeMatrix(travelTime);
        assertThat(travelTime.get(modelA, modelB)).isEqualTo(10L);
        assertThat(travelTime.get(modelB, modelA)).isEqualTo(20L);
    }

    @Test
    void recordingFailureDoesNotAffectTheCall() throws IOException {
        // A regular file where the recording directory should be, so nothing can be written.
        Path blocked = Files.createFile(tempDir.resolve("blocked"));
        var result = new TravelTimeAndDistanceWithMetadata(
                new TravelTimeAndDistance(matrix(0, 1, 1, 0), matrix(0, 1, 1, 0)), List.of());
        var service = new RecordingMapService(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return result;
            }
        }, new StaticDaypartBucketing(), blocked.resolve("distance-matrix"), mapper);

        assertThat(service.getTravelTimeAndDistance(List.of(a, b), OPTIONS)).isSameAs(result);
    }

    @Test
    void namesFilesByKindAndTransportType() throws IOException {
        var stub = new StubMapService() {
            @Override
            public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
                return List.of();
            }
        };
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS + ",transportType:car");
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS + ",transportType:bike");
        // A repeated call overwrites its recording.
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS + ",transportType:car");

        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder("locations-out-of-map-car.json", "locations-out-of-map-bike.json");
        }
    }

    /**
     * Fills a 2x2 matrix for {@code a} and {@code b}, using separate location instances, like the converters do.
     */
    private DistanceMatrix matrix(long aa, long ab, long ba, long bb) {
        var responseA = new Location(a.getLatitude(), a.getLongitude());
        var responseB = new Location(b.getLatitude(), b.getLongitude());
        DistanceMatrix matrix = DistanceMatrix.getInstance(2);
        matrix.put(responseA, responseA, aa);
        matrix.put(responseA, responseB, ab);
        matrix.put(responseB, responseA, ba);
        matrix.put(responseB, responseB, bb);
        return matrix;
    }

    /** Looks up with fresh location instances, as the matrix is matched by coordinates. */
    private long value(DistanceMatrix matrix, Location from, Location to) {
        return matrix.get(new Location(from.getLatitude(), from.getLongitude()),
                new Location(to.getLatitude(), to.getLongitude()));
    }

    private RecordingMapService recordingService(MapService delegate) {
        return new RecordingMapService(delegate, new StaticDaypartBucketing(), tempDir, mapper);
    }

    private Path singleFile(String suffix) throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> matches = files.filter(path -> path.getFileName().toString().endsWith(suffix)).toList();
            assertThat(matches).as("files ending with %s", suffix).hasSize(1);
            return matches.get(0);
        }
    }

    private abstract static class StubMapService implements MapService {

        @Override
        public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                String options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Location> getWaypoints(List<Location> locations, String options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
            throw new UnsupportedOperationException();
        }
    }
}
