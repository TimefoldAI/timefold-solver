package ai.timefold.solver.service.maps.service.client.impl.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.SingleTimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.StaticDaypartBucketing;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.Timeframe;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class ReplayMapServiceTest {

    private static final String CAR = "provider:osrm,location:us-georgia,transportType:car,tenantId:recorded";
    private static final String CAR_LOCALLY = "transportType:car,tenantId:local";
    private static final String BIKE = "provider:osrm,location:us-georgia,transportType:bike";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Location a = new Location(1.0, 2.0);
    private final Location b = new Location(3.0, 4.0);
    private final Location c = new Location(5.0, 6.0);
    private final List<Location> abc = List.of(a, b, c);

    @TempDir
    Path tempDir;

    @Test
    void replaysRecordedMatrices() {
        recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, 1), matrix(abc, 1000)), List.of(2), "us-georgia");
            }
        }).getTravelTimeAndDistance(abc, CAR);

        var replayed = replay().getTravelTimeAndDistance(List.of(new Location(1.0, 2.0), new Location(3.0, 4.0),
                new Location(5.0, 6.0)), CAR_LOCALLY);

        assertMatrix(replayed.travelTimeAndDistance().travelTime(), abc, 1);
        assertMatrix(replayed.travelTimeAndDistance().distance(), abc, 1000);
        assertThat(replayed.locationsNotInMapIdx()).containsExactly(2);
        assertThat(replayed.resolvedMapLocation()).isEqualTo("us-georgia");
    }

    @Test
    void doesNotTouchTheRequestedLocations() {
        recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, 1), matrix(abc, 1000)), List.of());
            }
        }).getTravelTimeAndDistance(abc, CAR);
        var modelA = new Location(1.0, 2.0);
        var modelB = new Location(3.0, 4.0);
        var modelC = new Location(5.0, 6.0);

        DistanceMatrix travelTime = replay().getTravelTimeAndDistance(List.of(modelA, modelB, modelC), CAR)
                .travelTimeAndDistance().travelTime();
        modelA.setTravelTimeMatrix(travelTime);
        modelB.setTravelTimeMatrix(travelTime);

        assertThat(travelTime.get(modelA, modelB)).isEqualTo(expected(a, b, 1));
        assertThat(travelTime.get(modelB, modelA)).isEqualTo(expected(b, a, 1));
    }

    @Test
    void matchesTransportType() {
        var recorder = recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                long factor = options.contains("bike") ? 7 : 1;
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, factor), matrix(abc, factor)), List.of());
            }
        });
        recorder.getTravelTimeAndDistance(abc, CAR);
        recorder.getTravelTimeAndDistance(abc, BIKE);

        var replay = replay();
        assertMatrix(replay.getTravelTimeAndDistance(abc, CAR).travelTimeAndDistance().travelTime(), abc, 1);
        assertMatrix(replay.getTravelTimeAndDistance(abc, BIKE).travelTimeAndDistance().travelTime(), abc, 7);
        assertThatThrownBy(() -> replay.getTravelTimeAndDistance(abc, "transportType:truck"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("truck");
    }

    @Test
    void replaysUnreachableRoutes() {
        long unreachable = Long.MAX_VALUE;
        recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                DistanceMatrix travelTime = matrix(abc, 1);
                travelTime.put(new Location(1.0, 2.0), new Location(5.0, 6.0), unreachable);
                return new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(travelTime, matrix(abc, 1)),
                        List.of());
            }
        }).getTravelTimeAndDistance(abc, CAR);

        DistanceMatrix travelTime = replay().getTravelTimeAndDistance(abc, CAR).travelTimeAndDistance().travelTime();

        assertThat(travelTime.get(new Location(1.0, 2.0), new Location(5.0, 6.0))).isEqualTo(unreachable);
        assertThat(travelTime.get(new Location(5.0, 6.0), new Location(1.0, 2.0))).isEqualTo(expected(c, a, 1));
    }

    @Test
    void ignoresMaxDistanceFromRoad() {
        recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, 1), matrix(abc, 1)), List.of());
            }
        }).getTravelTimeAndDistance(abc, CAR + ",maxDistanceFromRoad:999.0");

        // Set by the platform, typically not locally.
        assertMatrix(replay().getTravelTimeAndDistance(abc, CAR_LOCALLY).travelTimeAndDistance().travelTime(), abc, 1);
    }

    @Test
    void failsOnAnUnrecordedCall() {
        recorder(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, 1), matrix(abc, 1)), List.of());
            }
        }).getTravelTimeAndDistance(abc, CAR);

        // Only the distances were recorded, not the locations out of map.
        var replay = replay();
        assertThatThrownBy(() -> replay.getLocationsOutOfMap(abc, CAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No recording")
                .hasMessageContaining("locations-out-of-map-car.json");
    }

    @Test
    void replaysMatricesByTimeframe() {
        var bucketing = new StaticDaypartBucketing();
        recorder(new StubMapService() {
            @Override
            public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                    String options) {
                return new TravelTimesByTimeframeWithMetadata(
                        new DistanceMatrix[] { matrix(abc, 1), matrix(abc, 2), matrix(abc, 3) },
                        new DistanceMatrix[] { matrix(abc, 10), matrix(abc, 20), matrix(abc, 30) },
                        List.of(b), bucketing::indexOf);
            }
        }, bucketing).getTravelTimeAndDistanceByTimeframe(abc, CAR);

        var replayed = replay(bucketing).getTravelTimeAndDistanceByTimeframe(abc, CAR_LOCALLY);

        assertThat(replayed.travelTimesByTimeframe()).hasSize(3);
        for (int timeframe = 0; timeframe < 3; timeframe++) {
            assertMatrix(replayed.travelTimesByTimeframe()[timeframe], abc, timeframe + 1);
            assertMatrix(replayed.distancesByTimeframe()[timeframe], abc, 10L * (timeframe + 1));
        }
        assertThat(replayed.locationsNotInMap()).containsExactly(b);
        var afternoon = OffsetDateTime.of(2026, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        assertThat(replayed.timeframeIndexResolver().applyAsInt(afternoon)).isEqualTo(bucketing.indexOf(afternoon));
    }

    @Test
    void replaysSingleMatrixWithoutTraffic() {
        recorder(new StubMapService() {
            @Override
            public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                    String options) {
                return new TravelTimesByTimeframeWithMetadata(new DistanceMatrix[] { matrix(abc, 4) },
                        new DistanceMatrix[] { matrix(abc, 40) }, List.of(), time -> 0);
            }
        }).getTravelTimeAndDistanceByTimeframe(abc, CAR);

        var replayed = replay().getTravelTimeAndDistanceByTimeframe(abc, CAR);

        assertThat(replayed.travelTimesByTimeframe()).hasSize(1);
        assertMatrix(replayed.travelTimesByTimeframe()[0], abc, 4);
        assertThat(replayed.timeframeIndexResolver().applyAsInt(OffsetDateTime.now())).isZero();
    }

    @Test
    void failsOnDifferentTimeframes() {
        var bucketing = new StaticDaypartBucketing();
        recorder(new StubMapService() {
            @Override
            public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                    String options) {
                return new TravelTimesByTimeframeWithMetadata(
                        new DistanceMatrix[] { matrix(abc, 1), matrix(abc, 2), matrix(abc, 3) },
                        new DistanceMatrix[] { matrix(abc, 1), matrix(abc, 2), matrix(abc, 3) },
                        List.of(), bucketing::indexOf);
            }
        }, bucketing).getTravelTimeAndDistanceByTimeframe(abc, CAR);

        // Recorded with three timeframes, but the local run uses a different bucketing.
        var replay = new ReplayMapService(tempDir, new FourTimeframeBucketing(), new StubMapService() {
        }, mapper);
        assertThatThrownBy(() -> replay.getTravelTimeAndDistanceByTimeframe(abc, CAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[morning, afternoon, night]");
    }

    @Test
    void replaysLocationsOutOfMap() {
        var recorder = recorder(new StubMapService() {
            @Override
            public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
                return List.of(1);
            }
        });
        recorder.getLocationsOutOfMap(List.of(a, b), CAR);

        assertThat(replay().getLocationsOutOfMap(List.of(a, b), CAR_LOCALLY)).containsExactly(1);
    }

    @Test
    void acceptsTheUnzippedArtifactRoot() {
        Path artifactRoot = tempDir.resolve("artifact");
        var recorder = new RecordingMapService(new StubMapService() {
            @Override
            public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
                return new TravelTimeAndDistanceWithMetadata(
                        new TravelTimeAndDistance(matrix(abc, 1), matrix(abc, 1)), List.of());
            }
        }, new SingleTimeframeBucketing(), artifactRoot.resolve(RecordingMapService.DIRECTORY_NAME), mapper);
        recorder.getTravelTimeAndDistance(abc, CAR);

        var replay = new ReplayMapService(artifactRoot, new SingleTimeframeBucketing(), new StubMapService() {
        }, mapper);
        assertMatrix(replay.getTravelTimeAndDistance(abc, CAR).travelTimeAndDistance().travelTime(), abc, 1);
    }

    @Test
    void failsOnMissingOrEmptyDirectory() throws IOException {
        var missing = new ReplayMapService(tempDir.resolve("missing"), new SingleTimeframeBucketing(),
                new StubMapService() {
                }, mapper);
        assertThatThrownBy(() -> missing.getTravelTimeAndDistance(abc, CAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No recording");

        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        var emptyReplay = new ReplayMapService(empty, new SingleTimeframeBucketing(), new StubMapService() {
        }, mapper);
        assertThatThrownBy(() -> emptyReplay.getTravelTimeAndDistance(abc, CAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No recording");
    }

    @Test
    void delegatesWaypoints() {
        var replay = new ReplayMapService(tempDir, new SingleTimeframeBucketing(), new StubMapService() {
            @Override
            public List<Location> getWaypoints(List<Location> locations, String options) {
                return List.of(c);
            }
        }, mapper);

        assertThat(replay.getWaypoints(List.of(a, b), CAR)).containsExactly(c);
    }

    /**
     * Distinct values per (from, to) pair, so that a mix-up of indexes is detected.
     */
    private long expected(Location from, Location to, long factor) {
        return factor * (abc.indexOf(from) * 10L + abc.indexOf(to));
    }

    private DistanceMatrix matrix(List<Location> locations, long factor) {
        DistanceMatrix matrix = DistanceMatrix.getInstance(locations.size());
        for (Location from : locations) {
            for (Location to : locations) {
                matrix.put(new Location(from.getLatitude(), from.getLongitude()),
                        new Location(to.getLatitude(), to.getLongitude()), expected(from, to, factor));
            }
        }
        return matrix;
    }

    private void assertMatrix(DistanceMatrix matrix, List<Location> locations, long factor) {
        for (Location from : locations) {
            for (Location to : locations) {
                assertThat(matrix.get(new Location(from.getLatitude(), from.getLongitude()),
                        new Location(to.getLatitude(), to.getLongitude())))
                        .as("%s -> %s", from, to)
                        .isEqualTo(expected(from, to, factor));
            }
        }
    }

    private RecordingMapService recorder(MapService delegate) {
        return recorder(delegate, new SingleTimeframeBucketing());
    }

    private RecordingMapService recorder(MapService delegate, TimeframeBucketing bucketing) {
        return new RecordingMapService(delegate, bucketing, tempDir, mapper);
    }

    private ReplayMapService replay() {
        return replay(new SingleTimeframeBucketing());
    }

    private ReplayMapService replay(TimeframeBucketing bucketing) {
        return new ReplayMapService(tempDir, bucketing, new StubMapService() {
        }, mapper);
    }

    private static final class FourTimeframeBucketing extends StaticDaypartBucketing {

        @Override
        public List<Timeframe> allTimeframes() {
            return List.of(StaticDaypartBucketing.MORNING, StaticDaypartBucketing.AFTERNOON,
                    StaticDaypartBucketing.NIGHT,
                    new Timeframe("late-night"));
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
