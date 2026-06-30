package ai.timefold.solver.service.maps.service.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.service.maps.api.model.travel.TravelTime;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.StaticDaypartBucketing;
import ai.timefold.solver.service.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.junit.jupiter.api.Test;

class TravelTimeMatrixEnricherTrafficTest {

    private static final OffsetDateTime MORNING_AT = OffsetDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime AFTERNOON_AT = OffsetDateTime.of(2024, 1, 1, 16, 0, 0, 0, ZoneOffset.UTC);

    private final MapServiceOptionsSupplier optionsSupplier = new MapServiceOptionsSupplier(
            Optional.empty(), Optional.empty(), Optional.of(1000.0),
            Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    @Test
    void regularModelUsesSingleMatrix() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        DistanceMatrix travel = matrixOf(l1, l2, 75L);
        DistanceMatrix distance = matrixOf(l1, l2, 750L);
        StubMapService stub = new StubMapService(null,
                new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(travel, distance), List.of()));
        TravelTimeMatrixEnricher enricher =
                new TravelTimeMatrixEnricher(stub, optionsSupplier, false);

        enricher.enrich(new StubLocationsModel(List.of(l1, l2)));

        assertThat(stub.singleInvocationCount.get()).isEqualTo(1);
        assertThat(stub.byTimeframeInvocationCount.get()).isZero();
        assertThat(l1.getTravelTimeTo(l2)).isEqualTo(TravelTime.of(75L));
        assertThat(l1.getDistanceTo(l2)).isEqualTo(TravelDistance.of(750L));
    }

    @Test
    void regularModelWithTrafficEnabledFetchesAllTimeframes() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        DistanceMatrix morningTravel = matrixOf(l1, l2, 100L);
        DistanceMatrix morningDistance = matrixOf(l1, l2, 1_000L);
        DistanceMatrix afternoonTravel = matrixOf(l1, l2, 500L);
        DistanceMatrix afternoonDistance = matrixOf(l1, l2, 1_200L);

        StaticDaypartBucketing bucketing = new StaticDaypartBucketing();
        int n = bucketing.allTimeframes().size();
        DistanceMatrix[] travelTimesByTimeframe = new DistanceMatrix[n];
        DistanceMatrix[] distancesByTimeframe = new DistanceMatrix[n];
        int morningIdx = bucketing.indexOf(MORNING_AT);
        int afternoonIdx = bucketing.indexOf(AFTERNOON_AT);
        travelTimesByTimeframe[morningIdx] = morningTravel;
        distancesByTimeframe[morningIdx] = morningDistance;
        travelTimesByTimeframe[afternoonIdx] = afternoonTravel;
        distancesByTimeframe[afternoonIdx] = afternoonDistance;

        // Traffic on + plain model: the enricher fetches per-timeframe matrices for every location, no pruning.
        StubMapService stub = new StubMapService(new TravelTimesByAvailabilityWithMetadata(
                travelTimesByTimeframe, distancesByTimeframe, List.of(), bucketing::indexOf), null);
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(stub, optionsSupplier, true);

        enricher.enrich(new StubLocationsModel(List.of(l1, l2)));

        assertThat(stub.byTimeframeInvocationCount.get()).isEqualTo(1);
        assertThat(stub.singleInvocationCount.get()).isZero();
        // Every location is sent for the by-timeframe fetch (all locations in all buckets, no pruning).
        assertThat(stub.lastLocations).containsExactly(l1, l2);

        // Time-aware lookups resolve the per-timeframe matrices.
        assertThat(l1.getTravelTimeTo(l2, MORNING_AT)).isEqualTo(TravelTime.of(100L));
        assertThat(l1.getTravelTimeTo(l2, AFTERNOON_AT)).isEqualTo(TravelTime.of(500L));
        assertThat(l1.getDistanceTo(l2, AFTERNOON_AT)).isEqualTo(TravelDistance.of(1_200L));

        // Timestamp-less lookups fall back to the first available (morning) timeframe matrix.
        assertThat(l1.getTravelTimeTo(l2)).isEqualTo(TravelTime.of(100L));
        assertThat(l1.getDistanceTo(l2)).isEqualTo(TravelDistance.of(1_000L));
    }

    @Test
    void singleBucketResultUsesScalarMatrices() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        // When the map service returns a single-bucket result (e.g. a single-timeframe bucketing), the enricher stamps
        // the scalar matrices so lookups use the index-cache fast path.
        DistanceMatrix travel = matrixOf(l1, l2, 75L);
        DistanceMatrix distance = matrixOf(l1, l2, 750L);
        StubMapService stub = new StubMapService(new TravelTimesByAvailabilityWithMetadata(
                new DistanceMatrix[] { travel }, new DistanceMatrix[] { distance }, List.of(), t -> 0), null);
        TravelTimeMatrixEnricher enricher =
                new TravelTimeMatrixEnricher(stub, optionsSupplier, true);

        enricher.enrich(new StubLocationsModel(List.of(l1, l2)));

        // Both the timestamp-less and the time-aware overloads resolve to the single matrix.
        assertThat(l1.getTravelTimeTo(l2)).isEqualTo(TravelTime.of(75L));
        assertThat(l1.getDistanceTo(l2)).isEqualTo(TravelDistance.of(750L));
        assertThat(l1.getTravelTimeTo(l2, MORNING_AT)).isEqualTo(TravelTime.of(75L));
        assertThat(l1.getDistanceTo(l2, AFTERNOON_AT)).isEqualTo(TravelDistance.of(750L));
    }

    @Test
    void acceptsLocationsAwareSolverModel() {
        TravelTimeMatrixEnricher enricher =
                new TravelTimeMatrixEnricher(new StubMapService(null, null), optionsSupplier,
                        false);

        assertThat(enricher.accept(new StubLocationsModel(List.of()))).isTrue();
        assertThat(enricher.accept(new Object())).isFalse();
    }

    @Test
    void wrapsNonTimefoldExceptionFromMapService() {
        MapService failing = new FailingMapService();
        TravelTimeMatrixEnricher enricher =
                new TravelTimeMatrixEnricher(failing, optionsSupplier, true);

        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        assertThatThrownBy(() -> enricher.enrich(new StubLocationsModel(List.of(l1, l2))))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessageContaining("Error getting travel time and distances");
    }

    private static DistanceMatrix matrixOf(Location from, Location to, long value) {
        DistanceMatrix matrix = DistanceMatrix.getInstance(2);
        matrix.put(from, to, value);
        matrix.put(to, from, value);
        return matrix;
    }

    private static final class StubMapService implements MapService {

        private final TravelTimesByAvailabilityWithMetadata byTimeframeResult;
        private final TravelTimeAndDistanceWithMetadata singleResult;
        private final AtomicInteger byTimeframeInvocationCount = new AtomicInteger(0);
        private final AtomicInteger singleInvocationCount = new AtomicInteger(0);
        private List<Location> lastLocations;

        StubMapService(TravelTimesByAvailabilityWithMetadata byTimeframeResult,
                TravelTimeAndDistanceWithMetadata singleResult) {
            this.byTimeframeResult = byTimeframeResult;
            this.singleResult = singleResult;
        }

        @Override
        public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
            singleInvocationCount.incrementAndGet();
            if (singleResult == null) {
                throw new UnsupportedOperationException("single matrix result not configured");
            }
            return singleResult;
        }

        @Override
        public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                String options) {
            byTimeframeInvocationCount.incrementAndGet();
            lastLocations = locations;
            if (byTimeframeResult == null) {
                throw new UnsupportedOperationException("by-timeframe matrix result not configured");
            }
            return byTimeframeResult;
        }

        @Override
        public List<Location> getWaypoints(List<Location> locations, String options) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
            throw new UnsupportedOperationException("not used in this test");
        }
    }

    private static final class FailingMapService implements MapService {

        @Override
        public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
                String options) {
            throw new RuntimeException("boom");
        }

        @Override
        public List<Location> getWaypoints(List<Location> locations, String options) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
            throw new UnsupportedOperationException("not used in this test");
        }
    }

    private static class StubLocationsModel implements LocationsAwareSolverModel<HardSoftScore> {

        private final List<Location> locations;
        private List<Location> notInMap;

        StubLocationsModel(List<Location> locations) {
            this.locations = locations;
        }

        @Override
        public List<Location> getLocations() {
            return locations;
        }

        @Override
        public Optional<String> getLocationSetName() {
            return Optional.empty();
        }

        @Override
        public void setLocationsNotInMap(List<Location> locationsNotInMap) {
            this.notInMap = locationsNotInMap;
        }

        @Override
        public List<Location> getLocationsNotInMap() {
            return notInMap;
        }

        @Override
        public HardSoftScore getScore() {
            return null;
        }

        @Override
        public ConstraintWeightOverrides<HardSoftScore> getConstraintWeightOverrides() {
            return null;
        }
    }

}
