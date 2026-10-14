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

import com.fasterxml.jackson.databind.JsonNode;
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

        JsonNode json = mapper.readTree(singleFile("distances.json").toFile());
        assertThat(json.get("options").asText()).isEqualTo(OPTIONS);
        assertThat(json.get("locations").get(1).get("latitude").asDouble()).isEqualTo(3.0);
        assertThat(json.get("locationsNotInMap").get(0).asInt()).isEqualTo(1);
        assertThat(json.get("resolvedMapLocation").asText()).isEqualTo("us-georgia");
        assertThat(json.get("travelTime").toString()).isEqualTo("[[0,10],[20,0]]");
        assertThat(json.get("distance").toString()).isEqualTo("[[0,100],[200,0]]");
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

        JsonNode json = mapper.readTree(singleFile("distances-by-timeframe.json").toFile());
        assertThat(json.get("timeframes").toString()).isEqualTo("[\"morning\",\"afternoon\",\"night\"]");
        assertThat(json.get("locationsNotInMap").toString()).isEqualTo("[1]");
        assertThat(json.get("travelTimes").toString()).isEqualTo("[[[0,1],[2,0]],[[0,3],[4,0]],[[0,5],[6,0]]]");
        assertThat(json.get("distances").get(2).toString()).isEqualTo("[[0,7],[8,0]]");
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

        JsonNode json = mapper.readTree(singleFile("distances-by-timeframe.json").toFile());
        assertThat(json.get("timeframes").isNull()).isTrue();
        assertThat(json.get("travelTimes")).hasSize(1);
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

        JsonNode json = mapper.readTree(singleFile("locations-out-of-map.json").toFile());
        assertThat(json.get("locationsOutOfMap").toString()).isEqualTo("[0]");
        assertThat(json.get("locations")).hasSize(2);
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
    void callsAreNumberedAcrossInstances() throws IOException {
        var stub = new StubMapService() {
            @Override
            public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
                return List.of();
            }
        };
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS);
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS);

        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith("locations-out-of-map.json")))
                    .hasSize(2);
        }
    }

    @Test
    void recordsTheSameRequestOnce() throws IOException {
        var result = new TravelTimeAndDistanceWithMetadata(
                new TravelTimeAndDistance(matrix(0, 1, 1, 0), matrix(0, 1, 1, 0)), List.of());
        var service = recordingService(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return result;
            }
        });

        service.getTravelTimeAndDistance(List.of(a, b), OPTIONS);
        // Equal coordinates in new instances are the same request.
        service.getTravelTimeAndDistance(List.of(new Location(1.0, 2.0), new Location(3.0, 4.0)), OPTIONS);
        service.getTravelTimeAndDistance(List.of(b, a), OPTIONS);

        try (Stream<Path> files = Files.list(tempDir)) {
            // The reordered request is a different request.
            assertThat(files.filter(path -> path.getFileName().toString().endsWith("-distances.json"))).hasSize(2);
        }
    }

    @Test
    void keepsRecordingsOfAnEarlierRun() throws IOException {
        var stub = new StubMapService() {
            @Override
            public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
                return List.of();
            }
        };
        recordingService(stub).getLocationsOutOfMap(List.of(a), OPTIONS);
        Path earlier = singleFile("locations-out-of-map.json");
        String earlierContent = Files.readString(earlier);
        // The next file name of the sequence is already taken, as after a restart of the process.
        String taken = String.format("%05d-locations-out-of-map.json",
                Long.parseLong(earlier.getFileName().toString().substring(0, 5)) + 1);
        Files.writeString(tempDir.resolve(taken), "earlier run");

        recordingService(stub).getLocationsOutOfMap(List.of(b), OPTIONS);

        assertThat(Files.readString(earlier)).isEqualTo(earlierContent);
        assertThat(Files.readString(tempDir.resolve(taken))).isEqualTo("earlier run");
        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith("locations-out-of-map.json")))
                    .hasSize(3);
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
