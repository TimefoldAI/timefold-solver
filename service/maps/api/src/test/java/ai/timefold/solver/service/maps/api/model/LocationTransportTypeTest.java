package ai.timefold.solver.service.maps.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.ToIntFunction;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.service.maps.api.model.travel.TravelTime;

import org.junit.jupiter.api.Test;

class LocationTransportTypeTest {

    private static final int MORNING = 0;
    private static final int AFTERNOON = 1;

    private static final ToIntFunction<OffsetDateTime> INDEX_RESOLVER = at -> at.getHour() < 12 ? MORNING : AFTERNOON;

    private static final OffsetDateTime MORNING_AT = OffsetDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime AFTERNOON_AT = OffsetDateTime.of(2024, 1, 1, 16, 0, 0, 0, ZoneOffset.UTC);

    private final Location from = new Location(0, 0);
    private final Location to = new Location(1, 1);

    @Test
    void eachTransportTypeUsesItsOwnMatrices() {
        from.setTravelTimeMatrix(TransportType.CAR, matrix(100L));
        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setTravelTimeMatrix(TransportType.FOOT, matrix(900L));
        from.setDistanceMatrix(TransportType.CAR, matrix(1_000L));
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(1_100L));
        from.setDistanceMatrix(TransportType.FOOT, matrix(1_200L));

        assertThat(from.getTravelTimeTo(to, TransportType.CAR)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getTravelTimeTo(to, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThat(from.getTravelTimeTo(to, TransportType.FOOT)).isEqualTo(TravelTime.of(900L));
        assertThat(from.getDistanceTo(to, TransportType.CAR)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(from.getDistanceTo(to, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(1_100L));
        assertThat(from.getDistanceTo(to, TransportType.FOOT)).isEqualTo(TravelDistance.of(1_200L));
    }

    @Test
    void carAndNullShareTheTransportTypeLessMatrices() {
        from.setTravelTimeMatrix(matrix(100L));
        from.setDistanceMatrix(matrix(1_000L));

        assertThat(from.getTravelTimeTo(to, TransportType.CAR)).isEqualTo(from.getTravelTimeTo(to));
        assertThat(from.getTravelTimeTo(to, (TransportType) null)).isEqualTo(from.getTravelTimeTo(to));
        assertThat(from.getDistanceTo(to, TransportType.CAR)).isEqualTo(from.getDistanceTo(to));
        assertThat(from.getDistanceTo(to, (TransportType) null)).isEqualTo(from.getDistanceTo(to));
    }

    @Test
    void settingCarOrNullFeedsTheTransportTypeLessLookups() {
        from.setTravelTimeMatrix(TransportType.CAR, matrix(100L));
        from.setDistanceMatrix(null, matrix(1_000L));

        assertThat(from.getTravelTimeTo(to)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getDistanceTo(to)).isEqualTo(TravelDistance.of(1_000L));
    }

    @Test
    void nonDefaultTransportTypeDoesNotFeedTheTransportTypeLessLookups() {
        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(1_100L));

        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to));
    }

    @Test
    void transportTypeWithoutMatrixFails() {
        assertThatIllegalStateException()
                .isThrownBy(() -> from.getTravelTimeTo(to, TransportType.BICYCLE))
                .withMessageContaining("travel time")
                .withMessageContaining(TransportType.BICYCLE.toString());
        assertThatIllegalStateException()
                .isThrownBy(() -> from.getDistanceTo(to, TransportType.FOOT))
                .withMessageContaining("distance")
                .withMessageContaining(TransportType.FOOT.toString());
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, TransportType.CAR));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, TransportType.CAR));
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.BICYCLE));

        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(1_100L));
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, TransportType.FOOT));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, TransportType.FOOT));
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.FOOT));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.FOOT));
    }

    @Test
    void autoSelectIsNotARoutingProfile() {
        from.setTravelTimeMatrix(matrix(100L));
        from.setDistanceMatrix(matrix(1_000L));

        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, TransportType.AUTO_SELECT));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, TransportType.AUTO_SELECT));
    }

    @Test
    void routeMissingFromTheMatrixFailsWithTheTransportType() {
        from.setTravelTimeMatrix(TransportType.BICYCLE, new NoRoutesMatrix());
        from.setDistanceMatrix(TransportType.BICYCLE, new NoRoutesMatrix());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, TransportType.BICYCLE))
                .withMessageContaining(TransportType.BICYCLE.toString());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getDistanceTo(to, TransportType.BICYCLE))
                .withMessageContaining(TransportType.BICYCLE.toString());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE))
                .withMessageContaining(MORNING_AT.toString());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.BICYCLE))
                .withMessageContaining(MORNING_AT.toString());
    }

    @Test
    void timeframeLookupsWithoutTimeframeMatricesUseTheTransportTypeScalarMatrix() {
        from.setTravelTimeMatrix(TransportType.CAR, matrix(100L));
        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setDistanceMatrix(TransportType.CAR, matrix(1_000L));
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(1_100L));

        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.CAR)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.CAR)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(1_100L));
    }

    @Test
    void timeframeMatricesArePickedPerTransportType() {
        from.setTravelTimeMatrices(TransportType.CAR, timeframes(100L, 110L), INDEX_RESOLVER);
        from.setTravelTimeMatrices(TransportType.BICYCLE, timeframes(300L, 330L), INDEX_RESOLVER);
        from.setDistanceMatrices(TransportType.CAR, timeframes(1_000L, 1_100L), INDEX_RESOLVER);
        from.setDistanceMatrices(TransportType.BICYCLE, timeframes(2_000L, 2_200L), INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.CAR)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.CAR)).isEqualTo(TravelTime.of(110L));
        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(330L));
        assertThat(from.getDistanceTo(to, MORNING_AT, TransportType.CAR)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.CAR)).isEqualTo(TravelDistance.of(1_100L));
        assertThat(from.getDistanceTo(to, MORNING_AT, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(2_000L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(2_200L));
    }

    @Test
    void timeframeMatricesOfOneTransportTypeDoNotLeakIntoAnother() {
        from.setTravelTimeMatrices(TransportType.BICYCLE, timeframes(300L, 330L), INDEX_RESOLVER);
        from.setDistanceMatrices(TransportType.BICYCLE, timeframes(2_000L, 2_200L), INDEX_RESOLVER);
        from.setTravelTimeMatrix(TransportType.FOOT, matrix(900L));
        from.setDistanceMatrix(TransportType.FOOT, matrix(3_000L));

        // Foot has no timeframe matrices of its own, so the departure time does not matter for it.
        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.FOOT)).isEqualTo(TravelTime.of(900L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.FOOT)).isEqualTo(TravelTime.of(900L));
        assertThat(from.getDistanceTo(to, MORNING_AT, TransportType.FOOT)).isEqualTo(TravelDistance.of(3_000L));
        // Car has nothing set at all, the bicycle matrices must not answer for it.
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.CAR));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.CAR));
        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, TransportType.CAR));
    }

    @Test
    void nonDefaultTimeframeMatricesTakePrecedenceOverTheTransportTypeScalarMatrix() {
        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setTravelTimeMatrices(TransportType.BICYCLE, timeframes(310L, 330L), INDEX_RESOLVER);
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(2_000L));
        from.setDistanceMatrices(TransportType.BICYCLE, timeframes(2_100L, 2_200L), INDEX_RESOLVER);

        // Without a departure time the scalar matrix is used, with one the timeframe matrices are.
        assertThat(from.getTravelTimeTo(to, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(330L));
        assertThat(from.getDistanceTo(to, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(2_000L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(2_200L));
    }

    @Test
    void timeframeIndexOutOfBoundsFailsForNonDefaultTransportType() {
        ToIntFunction<OffsetDateTime> outOfBounds = at -> 5;
        from.setTravelTimeMatrices(TransportType.BICYCLE, timeframes(300L, 330L), outOfBounds);
        from.setDistanceMatrices(TransportType.BICYCLE, timeframes(2_000L, 2_200L), outOfBounds);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE))
                .withMessageContaining("out of bounds");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.BICYCLE))
                .withMessageContaining("out of bounds");
    }

    @Test
    void timeframeIndexOutOfBoundsFailsForDefaultTransportType() {
        ToIntFunction<OffsetDateTime> outOfBounds = at -> -1;
        from.setTravelTimeMatrices(TransportType.CAR, timeframes(100L, 110L), outOfBounds);
        from.setDistanceMatrices(TransportType.CAR, timeframes(1_000L, 1_100L), outOfBounds);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT, TransportType.CAR))
                .withMessageContaining("out of bounds");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getDistanceTo(to, MORNING_AT, TransportType.CAR))
                .withMessageContaining("out of bounds");
    }

    @Test
    void timeframeMatricesWithoutAnIndexResolverAreIgnored() {
        from.setTravelTimeMatrix(TransportType.CAR, matrix(100L));
        from.setTravelTimeMatrices(TransportType.CAR, timeframes(101L, 102L), null);
        from.setDistanceMatrix(TransportType.CAR, matrix(1_000L));
        from.setDistanceMatrices(TransportType.CAR, timeframes(1_001L, 1_002L), null);
        from.setTravelTimeMatrix(TransportType.BICYCLE, matrix(300L));
        from.setTravelTimeMatrices(TransportType.BICYCLE, timeframes(301L, 302L), null);
        from.setDistanceMatrix(TransportType.BICYCLE, matrix(2_000L));
        from.setDistanceMatrices(TransportType.BICYCLE, timeframes(2_001L, 2_002L), null);

        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.CAR)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.CAR)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT, TransportType.BICYCLE)).isEqualTo(TravelDistance.of(2_000L));
    }

    @Test
    void missingTimeframeMatrixFailsForNonDefaultTransportType() {
        DistanceMatrix[] onlyMorning = new DistanceMatrix[2];
        onlyMorning[MORNING] = matrix(300L);
        from.setTravelTimeMatrices(TransportType.BICYCLE, onlyMorning, INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to, MORNING_AT, TransportType.BICYCLE)).isEqualTo(TravelTime.of(300L));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, AFTERNOON_AT, TransportType.BICYCLE))
                .withMessageContaining("No travel time matrix fetched");
    }

    @Test
    void defaultTransportTypeTimeframeMatricesFeedTheTransportTypeLessLookups() {
        from.setTravelTimeMatrices(TransportType.CAR, timeframes(100L, 110L), INDEX_RESOLVER);
        from.setDistanceMatrices(null, timeframes(1_000L, 1_100L), INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT)).isEqualTo(TravelTime.of(110L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT)).isEqualTo(TravelDistance.of(1_100L));
    }

    private DistanceMatrix matrix(long value) {
        DistanceMatrix matrix = DistanceMatrix.getInstance(2);
        matrix.put(from, to, value);
        return matrix;
    }

    private DistanceMatrix[] timeframes(long morning, long afternoon) {
        DistanceMatrix[] matrices = new DistanceMatrix[2];
        matrices[MORNING] = matrix(morning);
        matrices[AFTERNOON] = matrix(afternoon);
        return matrices;
    }

    // A matrix that has no information about any route.
    private static final class NoRoutesMatrix implements DistanceMatrix {

        @Override
        public long get(Location from, Location to) {
            return -1L;
        }

        @Override
        public void put(Location from, Location to, long distance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getNumberOfOriginLocations() {
            return 0;
        }

        @Override
        public int getNumberOfDestinationLocations() {
            return 0;
        }
    }
}
