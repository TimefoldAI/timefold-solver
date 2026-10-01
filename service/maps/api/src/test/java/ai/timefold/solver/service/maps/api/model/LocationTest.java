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

class LocationTest {

    // Hour-based stand-in bucketing for the tests: 0 = morning, 1 = afternoon, 2 = night.
    private static final int MORNING = 0;
    private static final int AFTERNOON = 1;
    private static final int NIGHT = 2;

    private static final ToIntFunction<OffsetDateTime> INDEX_RESOLVER = at -> {
        int hour = at.getHour();
        if (hour < 12)
            return MORNING;
        if (hour < 18)
            return AFTERNOON;
        return NIGHT;
    };

    private static final OffsetDateTime MORNING_AT = OffsetDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime AFTERNOON_AT = OffsetDateTime.of(2024, 1, 1, 16, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime NIGHT_AT = OffsetDateTime.of(2024, 1, 1, 22, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void picksCorrectTravelTimeMatrixPerTimeframe() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        DistanceMatrix morningTravel = DistanceMatrix.getInstance(2);
        morningTravel.put(from, to, 100L);
        DistanceMatrix afternoonTravel = DistanceMatrix.getInstance(2);
        afternoonTravel.put(from, to, 500L);

        DistanceMatrix[] travelByTimeframe = new DistanceMatrix[3];
        travelByTimeframe[MORNING] = morningTravel;
        travelByTimeframe[AFTERNOON] = afternoonTravel;

        from.setTravelTimeMatrices(travelByTimeframe, INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to, MORNING_AT)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getTravelTimeTo(to, AFTERNOON_AT)).isEqualTo(TravelTime.of(500L));
    }

    @Test
    void picksCorrectDistanceMatrixPerTimeframe() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        DistanceMatrix morningDistance = DistanceMatrix.getInstance(2);
        morningDistance.put(from, to, 1_000L);
        DistanceMatrix afternoonDistance = DistanceMatrix.getInstance(2);
        afternoonDistance.put(from, to, 1_200L);

        DistanceMatrix[] distanceByTimeframe = new DistanceMatrix[3];
        distanceByTimeframe[MORNING] = morningDistance;
        distanceByTimeframe[AFTERNOON] = afternoonDistance;

        from.setDistanceMatrices(distanceByTimeframe, INDEX_RESOLVER);

        assertThat(from.getDistanceTo(to, MORNING_AT)).isEqualTo(TravelDistance.of(1_000L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT)).isEqualTo(TravelDistance.of(1_200L));
    }

    @Test
    void timeframelessAndTimeframeOverloadsUseTheirOwnMatrices() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        DistanceMatrix singleTravel = DistanceMatrix.getInstance(2);
        singleTravel.put(from, to, 10L);
        DistanceMatrix singleDistance = DistanceMatrix.getInstance(2);
        singleDistance.put(from, to, 20L);
        DistanceMatrix morningTravel = DistanceMatrix.getInstance(2);
        morningTravel.put(from, to, 100L);
        DistanceMatrix morningDistance = DistanceMatrix.getInstance(2);
        morningDistance.put(from, to, 1_000L);

        DistanceMatrix[] travelByTimeframe = new DistanceMatrix[3];
        travelByTimeframe[MORNING] = morningTravel;
        DistanceMatrix[] distanceByTimeframe = new DistanceMatrix[3];
        distanceByTimeframe[MORNING] = morningDistance;

        from.setTravelTimeMatrix(singleTravel);
        from.setDistanceMatrix(singleDistance);
        from.setTravelTimeMatrices(travelByTimeframe, INDEX_RESOLVER);
        from.setDistanceMatrices(distanceByTimeframe, INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to)).isEqualTo(TravelTime.of(10L));
        assertThat(from.getDistanceTo(to)).isEqualTo(TravelDistance.of(20L));
        assertThat(from.getTravelTimeTo(to, MORNING_AT)).isEqualTo(TravelTime.of(100L));
        assertThat(from.getDistanceTo(to, MORNING_AT)).isEqualTo(TravelDistance.of(1_000L));
    }

    @Test
    void timeframelessLookupFallsBackToFirstTimeframeMatrix() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        // Only per-timeframe matrices configured, no single matrix. The timestamp-less overloads must still work,
        // falling back to the first available timeframe matrix.
        DistanceMatrix afternoonTravel = DistanceMatrix.getInstance(2);
        afternoonTravel.put(from, to, 500L);
        DistanceMatrix afternoonDistance = DistanceMatrix.getInstance(2);
        afternoonDistance.put(from, to, 1_200L);

        DistanceMatrix[] travelByTimeframe = new DistanceMatrix[3];
        travelByTimeframe[AFTERNOON] = afternoonTravel;
        DistanceMatrix[] distanceByTimeframe = new DistanceMatrix[3];
        distanceByTimeframe[AFTERNOON] = afternoonDistance;

        from.setTravelTimeMatrices(travelByTimeframe, INDEX_RESOLVER);
        from.setDistanceMatrices(distanceByTimeframe, INDEX_RESOLVER);

        assertThat(from.getTravelTimeTo(to)).isEqualTo(TravelTime.of(500L));
        assertThat(from.getDistanceTo(to)).isEqualTo(TravelDistance.of(1_200L));
    }

    @Test
    void timeframeLookupFallsBackToSingleMatrix() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        // Only a single matrix configured, no per-timeframe matrices. The timeframe overloads must still work,
        // falling back to the single matrix and ignoring the departure time.
        DistanceMatrix singleTravel = DistanceMatrix.getInstance(2);
        singleTravel.put(from, to, 10L);
        DistanceMatrix singleDistance = DistanceMatrix.getInstance(2);
        singleDistance.put(from, to, 20L);

        from.setTravelTimeMatrix(singleTravel);
        from.setDistanceMatrix(singleDistance);

        assertThat(from.getTravelTimeTo(to, MORNING_AT)).isEqualTo(TravelTime.of(10L));
        assertThat(from.getTravelTimeTo(to, NIGHT_AT)).isEqualTo(TravelTime.of(10L));
        assertThat(from.getDistanceTo(to, AFTERNOON_AT)).isEqualTo(TravelDistance.of(20L));
    }

    @Test
    void missingTimeframeCellThrows() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        DistanceMatrix morningTravel = DistanceMatrix.getInstance(2);
        morningTravel.put(from, to, 100L);
        DistanceMatrix[] travelByTimeframe = new DistanceMatrix[3];
        travelByTimeframe[MORNING] = morningTravel;
        from.setTravelTimeMatrices(travelByTimeframe, INDEX_RESOLVER);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, NIGHT_AT))
                .withMessageContaining("No travel time matrix fetched");
    }

    @Test
    void unconfiguredTimeframeLookupThrows() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        assertThatIllegalStateException().isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT));
        assertThatIllegalStateException().isThrownBy(() -> from.getDistanceTo(to, MORNING_AT));
    }

    @Test
    void outOfBoundsIndexThrows() {
        Location from = new Location(0, 0);
        Location to = new Location(1, 1);

        DistanceMatrix matrix = DistanceMatrix.getInstance(2);
        matrix.put(from, to, 100L);
        DistanceMatrix[] byTimeframe = new DistanceMatrix[] { matrix };
        from.setTravelTimeMatrices(byTimeframe, at -> 5);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> from.getTravelTimeTo(to, MORNING_AT))
                .withMessageContaining("out of bounds");
    }

}
