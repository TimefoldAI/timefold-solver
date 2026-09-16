package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.Random;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class SlotReservationMapTest {

    private static final int MAX_SPARSE_ENTRY_COUNT = 16; // Mirrors the constant in SlotReservationMap.

    /**
     * The main coverage: a {@link HashMap} answers the same questions,
     * over both stages and across the upgrade between them.
     * It needs no special case for an identity reservation,
     * because {@code put(5, 5)} and {@code remove(5)} both give {@code getOrDefault(5, 5) == 5}.
     */
    @Test
    void matchesHashMapOracle() {
        var random = new Random(0);
        var slotCount = 200; // Large enough for MAX_SPARSE_ENTRY_COUNT to be the binding cap.
        var map = new SlotReservationMap(slotCount);
        var oracle = new HashMap<Integer, Integer>();
        for (var operation = 0; operation < 10_000; operation++) {
            var slot = random.nextInt(slotCount);
            if (random.nextInt(3) == 0) {
                map.release(slot);
                oracle.remove(slot);
            } else {
                var logicalIndex = random.nextInt(slotCount);
                map.reserve(slot, logicalIndex);
                oracle.put(slot, logicalIndex);
            }
            var actual = new int[slotCount];
            var expected = new int[slotCount];
            for (var candidate = 0; candidate < slotCount; candidate++) {
                actual[candidate] = map.resolve(candidate);
                expected[candidate] = oracle.getOrDefault(candidate, candidate);
            }
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test
    void upgradesToDenseOnlyWhenTheLogFillsUp() {
        var slotCount = 200;
        var map = new SlotReservationMap(slotCount);
        for (var slot = 0; slot < MAX_SPARSE_ENTRY_COUNT; slot++) {
            map.reserve(slot, slot + 100);
            assertThat(map.isDense())
                    .as("Reservation %d of %d must still fit the log.", slot + 1, MAX_SPARSE_ENTRY_COUNT)
                    .isFalse();
        }

        map.reserve(MAX_SPARSE_ENTRY_COUNT, MAX_SPARSE_ENTRY_COUNT + 100);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isTrue();
            // The upgrade must replay every logged reservation into the dense array.
            for (var slot = 0; slot <= MAX_SPARSE_ENTRY_COUNT; slot++) {
                softly.assertThat(map.resolve(slot)).isEqualTo(slot + 100);
            }
            for (var slot = MAX_SPARSE_ENTRY_COUNT + 1; slot < slotCount; slot++) {
                softly.assertThat(map.resolve(slot)).isEqualTo(slot);
            }
        });
    }

    @Test
    void reservingTheSameSlotAgainDoesNotGrowTheLog() {
        var map = new SlotReservationMap(200);
        for (var logicalIndex = 100; logicalIndex < 120; logicalIndex++) {
            map.reserve(5, logicalIndex);
        }

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isFalse();
            softly.assertThat(map.resolve(5)).isEqualTo(119);
        });
    }

    @Test
    void identityReservationsDoNotFillTheLog() {
        var map = new SlotReservationMap(200);
        for (var slot = 0; slot < 20; slot++) { // Without the short-circuit, the 17th would upgrade.
            map.reserve(slot, slot);
        }

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isFalse();
            for (var slot = 0; slot < 20; slot++) {
                softly.assertThat(map.resolve(slot)).isEqualTo(slot);
            }
        });
    }

    @Test
    void anIdentityReservationClearsAnExistingOne() {
        var map = new SlotReservationMap(200);
        map.reserve(5, 100);
        map.reserve(5, 5);

        assertThat(map.resolve(5)).isEqualTo(5);
    }

    @Test
    void denseStageDistinguishesLogicalIndexZeroFromTheIdentity() {
        var map = forceDense(new SlotReservationMap(200));

        map.reserve(3, 0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isTrue();
            softly.assertThat(map.resolve(3)).isZero();
            softly.assertThat(map.resolve(0)).isEqualTo(100); // Untouched by the reservation above.
        });
    }

    @Test
    void releaseRestoresTheIdentityInBothStages() {
        var sparseMap = new SlotReservationMap(200);
        sparseMap.reserve(5, 100);
        sparseMap.release(5);

        var denseMap = forceDense(new SlotReservationMap(200));
        denseMap.release(3);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sparseMap.isDense()).isFalse();
            softly.assertThat(sparseMap.resolve(5)).isEqualTo(5);
            softly.assertThat(denseMap.isDense()).isTrue();
            softly.assertThat(denseMap.resolve(3)).isEqualTo(3);
            softly.assertThat(denseMap.resolve(4)).isEqualTo(104); // Its neighbours survive.
        });
    }

    @Test
    void singleSlotSourceNeverDeviatesFromTheIdentity() {
        var map = new SlotReservationMap(1);

        map.reserve(0, 0); // The only reservation the range allows is the identity.

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isFalse();
            softly.assertThat(map.resolve(0)).isZero();
        });
    }

    @Test
    void twoSlotSourceUpgradesOnTheSecondReservation() {
        var map = new SlotReservationMap(2); // maxSparseEntryCount is 1 here, not 16.

        map.reserve(0, 1);
        assertThat(map.isDense()).isFalse();

        map.reserve(1, 0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isTrue();
            softly.assertThat(map.resolve(0)).isEqualTo(1);
            softly.assertThat(map.resolve(1)).isZero();
        });
    }

    @Test
    void fourSlotSourceUpgradesOnTheThirdReservation() {
        var map = new SlotReservationMap(4); // maxSparseEntryCount is 2 here.

        map.reserve(0, 1);
        map.reserve(1, 2);
        assertThat(map.isDense()).isFalse();

        map.reserve(2, 3);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isTrue();
            softly.assertThat(map.resolve(0)).isEqualTo(1);
            softly.assertThat(map.resolve(1)).isEqualTo(2);
            softly.assertThat(map.resolve(2)).isEqualTo(3);
            softly.assertThat(map.resolve(3)).isEqualTo(3);
        });
    }

    @Test
    void emptySourceAcceptsNoSlotAtAll() {
        var map = new SlotReservationMap(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(map.isDense()).isFalse();
            softly.assertThat(map).hasToString("{}");
            softly.assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> map.reserve(0, 0));
            softly.assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> map.release(0));
        });
    }

    @Test
    void negativeSlotCountIsRejected() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new SlotReservationMap(-1))
                .withMessageContaining("slotCount (-1)");
    }

    @Test
    void outOfRangeArgumentsAreRejectedInBothStages() {
        var sparseMap = new SlotReservationMap(200);
        var denseMap = forceDense(new SlotReservationMap(200));

        SoftAssertions.assertSoftly(softly -> {
            for (var map : new SlotReservationMap[] { sparseMap, denseMap }) {
                softly.assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> map.reserve(200, 0))
                        .withMessageContaining("The slot (200)");
                softly.assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> map.reserve(-1, 0))
                        .withMessageContaining("The slot (-1)");
                softly.assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> map.reserve(0, 200))
                        .withMessageContaining("The logicalIndex (200)");
                softly.assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> map.release(200))
                        .withMessageContaining("The slot (200)");
            }
        });
    }

    @Test
    void toStringRendersTheReservationsInBothStages() {
        var emptyMap = new SlotReservationMap(200);
        var sparseMap = new SlotReservationMap(200);
        sparseMap.reserve(3, 7);
        var denseMap = new SlotReservationMap(2);
        denseMap.reserve(0, 1);
        denseMap.reserve(1, 0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(emptyMap).hasToString("{}");
            softly.assertThat(sparseMap).hasToString("{3=7}");
            softly.assertThat(denseMap).hasToString("{0=1, 1=0}");
        });
    }

    /**
     * Reserves {@code slot -> slot + 100} for enough slots to overflow the log,
     * so the returned map is in the dense stage.
     */
    private static SlotReservationMap forceDense(SlotReservationMap map) {
        for (var slot = 0; slot <= MAX_SPARSE_ENTRY_COUNT; slot++) {
            map.reserve(slot, slot + 100);
        }
        return map;
    }

}
