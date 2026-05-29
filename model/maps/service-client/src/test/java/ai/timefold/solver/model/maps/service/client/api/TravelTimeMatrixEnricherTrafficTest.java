package ai.timefold.solver.model.maps.service.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.maps.api.DistanceMatrix;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.TimeInterval;
import ai.timefold.solver.model.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.model.maps.api.model.travel.TravelTime;
import ai.timefold.solver.model.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.model.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.model.maps.service.client.impl.bucketing.StaticDaypartBucketing;
import ai.timefold.solver.model.maps.service.integration.api.LocationsAndTrafficAwareSolverModel;
import ai.timefold.solver.model.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.junit.jupiter.api.Test;

class TravelTimeMatrixEnricherTrafficTest {

    private static final OffsetDateTime MORNING_AT = OffsetDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime AFTERNOON_AT = OffsetDateTime.of(2024, 1, 1, 16, 0, 0, 0, ZoneOffset.UTC);

    private final MapServiceOptionsSupplier optionsSupplier = new MapServiceOptionsSupplier(
            Optional.empty(), Optional.empty(), Optional.of(1000.0),
            Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    @Test
    void stampsMatricesAndResolverOntoEveryLocationForTrafficAwareModel() {
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
        ToIntFunction<OffsetDateTime> resolver = bucketing::indexOf;

        StubMapService stub = new StubMapService(new TravelTimesByAvailabilityWithMetadata(
                travelTimesByTimeframe, distancesByTimeframe, List.of(), resolver), null);
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(stub, optionsSupplier);

        Map<Location, List<TimeInterval>> availability = new LinkedHashMap<>();
        // Single interval spanning morning and afternoon covers both timeframes
        availability.put(l1, List.of(new TimeInterval(MORNING_AT, AFTERNOON_AT)));
        availability.put(l2, List.of(new TimeInterval(MORNING_AT, AFTERNOON_AT)));

        LocationsAwareSolverModel<?> enriched = enricher.enrich(new StubTrafficModel(availability));

        assertThat(stub.trafficInvocationCount.get()).isEqualTo(1);
        assertThat(stub.singleInvocationCount.get()).isZero();
        assertThat(stub.lastAvailability).isEqualTo(availability);

        assertThat(l1.getTravelTimeTo(l2, MORNING_AT)).isEqualTo(TravelTime.of(100L));
        assertThat(l1.getTravelTimeTo(l2, AFTERNOON_AT)).isEqualTo(TravelTime.of(500L));
        assertThat(l1.getDistanceTo(l2, MORNING_AT)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(l1.getDistanceTo(l2, AFTERNOON_AT)).isEqualTo(TravelDistance.of(1_200L));

        assertThat(l2.getTravelTimeTo(l1, MORNING_AT)).isEqualTo(TravelTime.of(100L));
        assertThat(l2.getDistanceTo(l1, AFTERNOON_AT)).isEqualTo(TravelDistance.of(1_200L));

        assertThat(enriched.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void regularModelUsesSingleMatrix() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        DistanceMatrix travel = matrixOf(l1, l2, 75L);
        DistanceMatrix distance = matrixOf(l1, l2, 750L);
        StubMapService stub = new StubMapService(null,
                new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(travel, distance), List.of()));
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(stub, optionsSupplier);

        enricher.enrich(new StubLocationsModel(List.of(l1, l2)));

        assertThat(stub.singleInvocationCount.get()).isEqualTo(1);
        assertThat(stub.trafficInvocationCount.get()).isZero();
        assertThat(l1.getTravelTimeTo(l2)).isEqualTo(TravelTime.of(75L));
        assertThat(l1.getDistanceTo(l2)).isEqualTo(TravelDistance.of(750L));
    }

    @Test
    void trafficAwareModelIsModelLevelSwitch() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        StaticDaypartBucketing bucketing = new StaticDaypartBucketing();
        DistanceMatrix[] travelTimesByTimeframe = new DistanceMatrix[bucketing.allTimeframes().size()];
        DistanceMatrix[] distancesByTimeframe = new DistanceMatrix[bucketing.allTimeframes().size()];
        int morningIdx = bucketing.indexOf(MORNING_AT);
        travelTimesByTimeframe[morningIdx] = matrixOf(l1, l2, 100L);
        distancesByTimeframe[morningIdx] = matrixOf(l1, l2, 1_000L);

        StubMapService stub = new StubMapService(new TravelTimesByAvailabilityWithMetadata(
                travelTimesByTimeframe, distancesByTimeframe, List.of(), bucketing::indexOf), null);
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(stub, optionsSupplier);

        Map<Location, List<TimeInterval>> availability =
                Map.of(l1, List.of(new TimeInterval(MORNING_AT, MORNING_AT)),
                        l2, List.of(new TimeInterval(MORNING_AT, MORNING_AT)));
        enricher.enrich(new StubTrafficModel(availability));

        assertThat(stub.trafficInvocationCount.get()).isEqualTo(1);
        assertThat(stub.lastAvailability).isEqualTo(availability);
        assertThat(l1.getTravelTimeTo(l2, MORNING_AT)).isEqualTo(TravelTime.of(100L));
    }

    @Test
    void acceptsLocationsAwareSolverModel() {
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(new StubMapService(null, null), optionsSupplier);

        assertThat(enricher.accept(new StubLocationsModel(List.of()))).isTrue();
        assertThat(enricher.accept(new Object())).isFalse();
    }

    @Test
    void wrapsNonTimefoldExceptionFromMapService() {
        MapService failing = new FailingMapService();
        TravelTimeMatrixEnricher enricher = new TravelTimeMatrixEnricher(failing, optionsSupplier);

        Location l1 = new Location(0, 0);
        Map<Location, List<TimeInterval>> availability = new LinkedHashMap<>();
        availability.put(l1, List.of(new TimeInterval(MORNING_AT, MORNING_AT)));

        assertThatThrownBy(() -> enricher.enrich(new StubTrafficModel(availability)))
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

        private final TravelTimesByAvailabilityWithMetadata trafficResult;
        private final TravelTimeAndDistanceWithMetadata singleResult;
        private final AtomicInteger trafficInvocationCount = new AtomicInteger(0);
        private final AtomicInteger singleInvocationCount = new AtomicInteger(0);
        private Map<? extends Location, List<TimeInterval>> lastAvailability;

        StubMapService(TravelTimesByAvailabilityWithMetadata trafficResult,
                TravelTimeAndDistanceWithMetadata singleResult) {
            this.trafficResult = trafficResult;
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
        public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistance(List<Location> locations, String options,
                Map<Location, List<TimeInterval>> timeAvailability) {
            trafficInvocationCount.incrementAndGet();
            lastAvailability = timeAvailability;
            if (trafficResult == null) {
                throw new UnsupportedOperationException("traffic matrix result not configured");
            }
            return trafficResult;
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
        public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistance(List<Location> locations, String options,
                Map<Location, List<TimeInterval>> timeAvailability) {
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

    private static final class StubTrafficModel extends StubLocationsModel
            implements LocationsAndTrafficAwareSolverModel<HardSoftScore> {

        private final Map<Location, List<TimeInterval>> availability;

        StubTrafficModel(Map<Location, List<TimeInterval>> availability) {
            super(List.copyOf(availability.keySet()));
            this.availability = availability;
        }

        @Override
        public Map<Location, List<TimeInterval>> getLocationsWithTimeAvailability() {
            return availability;
        }
    }

}
