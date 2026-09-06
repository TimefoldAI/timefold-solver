package ai.timefold.solver.core.impl.solver.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

class BoundedSplittableGeneratorTest {

    private static final int SAMPLE_COUNT = 300_000;

    private static BoundedSplittableGenerator generator() {
        return new BoundedSplittableGenerator(new SplittableRandom(0L));
    }

    @Test
    void boundedIntStaysInRange() {
        var random = generator();
        // Powers of two, primes, a bound above 2^30, and the maximum.
        for (var bound : new int[] { 1, 2, 3, 7, 64, 1_000, 65_537, 1_500_000_000, Integer.MAX_VALUE }) {
            for (var i = 0; i < 20_000; i++) {
                assertThat(random.nextInt(bound))
                        .as("bound %d", bound)
                        .isBetween(0, bound - 1);
            }
        }
    }

    @Test
    void boundedIntIsUniform() {
        var random = generator();
        var bound = 3; // Not a power of two, therefore the rejection path applies.
        var counts = new int[bound];
        for (var i = 0; i < SAMPLE_COUNT; i++) {
            counts[random.nextInt(bound)]++;
        }
        var expected = SAMPLE_COUNT / bound;
        for (var count : counts) {
            assertThat(count).isBetween((int) (expected * 0.98), (int) (expected * 1.02));
        }
    }

    @Test
    void boundedIntRejectsNonPositiveBound() {
        var random = generator();
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextInt(0));
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextInt(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextInt(Integer.MIN_VALUE));
    }

    @Test
    void rangedIntStaysInRange() {
        var random = generator();
        for (var i = 0; i < 100_000; i++) {
            assertThat(random.nextInt(-5, 6)).isBetween(-5, 5);
        }
        // The range does not fit in an int; the delegate handles it.
        for (var i = 0; i < 1_000; i++) {
            assertThat(random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE))
                    .isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE - 1);
        }
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextInt(5, 5));
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextInt(6, 5));
    }

    @Test
    void boundedLongStaysInRange() {
        var random = generator();
        for (var bound : new long[] { 1L, 2L, 3L, 64L, 1_000_000_007L, 1L << 62, Long.MAX_VALUE }) {
            for (var i = 0; i < 20_000; i++) {
                assertThat(random.nextLong(bound))
                        .as("bound %d", bound)
                        .isBetween(0L, bound - 1L);
            }
        }
    }

    @Test
    void boundedLongIsUniform() {
        var random = generator();
        var bound = 3L;
        var counts = new int[(int) bound];
        for (var i = 0; i < SAMPLE_COUNT; i++) {
            counts[(int) random.nextLong(bound)]++;
        }
        var expected = SAMPLE_COUNT / (int) bound;
        for (var count : counts) {
            assertThat(count).isBetween((int) (expected * 0.98), (int) (expected * 1.02));
        }
    }

    @Test
    void boundedLongRejectsNonPositiveBound() {
        var random = generator();
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextLong(0L));
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextLong(-1L));
    }

    @Test
    void rangedLongStaysInRange() {
        var random = generator();
        for (var i = 0; i < 100_000; i++) {
            assertThat(random.nextLong(-5L, 6L)).isBetween(-5L, 5L);
        }
        // The range does not fit in a long; the delegate handles it.
        for (var i = 0; i < 1_000; i++) {
            assertThat(random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE))
                    .isBetween(Long.MIN_VALUE, Long.MAX_VALUE - 1L);
        }
        assertThatIllegalArgumentException().isThrownBy(() -> random.nextLong(5L, 5L));
    }

    @Test
    void boundedIntStreamStaysInRangeAndKeepsCharacteristics() {
        var random = generator();
        assertThat(random.ints(10, 100).limit(10_000))
                .allSatisfy(value -> assertThat(value).isBetween(10, 99));

        assertThat(random.ints(500L, 10, 100)).hasSize(500);

        var spliterator = random.ints(500L, 10, 100).spliterator();
        assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isTrue();
        assertThat(spliterator.hasCharacteristics(Spliterator.ORDERED)).isTrue();
        assertThat(spliterator.estimateSize()).isEqualTo(500L);

        assertThatIllegalArgumentException().isThrownBy(() -> random.ints(-1L, 10, 100));
    }

    @Test
    void boundedLongStreamStaysInRangeAndKeepsCharacteristics() {
        var random = generator();
        assertThat(random.longs(10L, 100L).limit(10_000))
                .allSatisfy(value -> assertThat(value).isBetween(10L, 99L));

        assertThat(random.longs(500L, 10L, 100L)).hasSize(500);

        var spliterator = random.longs(500L, 10L, 100L).spliterator();
        assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isTrue();
        assertThat(spliterator.hasCharacteristics(Spliterator.ORDERED)).isTrue();

        assertThatIllegalArgumentException().isThrownBy(() -> random.longs(-1L, 10L, 100L));
    }

    @Test
    void boundingSurvivesEverySplit() {
        var random = generator();
        var split = random.split();
        assertThat(split).isInstanceOf(BoundedSplittableGenerator.class);
        assertThat(((RandomGenerator.SplittableGenerator) split).split())
                .isInstanceOf(BoundedSplittableGenerator.class);
        assertThat(random.split(random)).isInstanceOf(BoundedSplittableGenerator.class);
        assertThat(random.splits(3L)).hasSize(3)
                .allSatisfy(each -> assertThat(each).isInstanceOf(BoundedSplittableGenerator.class));
        assertThat(random.splits(3L, random)).hasSize(3)
                .allSatisfy(each -> assertThat(each).isInstanceOf(BoundedSplittableGenerator.class));
    }

    @Test
    void randomSourceAlwaysHandsOutBoundedGenerators() {
        var randomSource = DefaultRandomSource.seeded(0L);
        assertBounded(randomSource);
        assertBounded(randomSource.split());

        randomSource.restoreState(randomSource.saveState());
        assertBounded(randomSource);
    }

    private static void assertBounded(DefaultRandomSource randomSource) {
        assertThat(randomSource.moveRandom().getDelegate()).isInstanceOf(BoundedSplittableGenerator.class);
        assertThat(randomSource.acceptorRandom().getDelegate()).isInstanceOf(BoundedSplittableGenerator.class);
    }

    @Test
    void delegatingGeneratorBoundsItsStreamsAndKeepsTheOwnerCheck() throws Exception {
        var random = new DelegatingSplittableRandomGenerator(0L);
        assertThat(random.ints(10, 100).limit(5_000))
                .allSatisfy(value -> assertThat(value).isBetween(10, 99));
        assertThat(random.ints(500L, 10, 100)).hasSize(500);
        assertThat(random.longs(10L, 100L).limit(5_000))
                .allSatisfy(value -> assertThat(value).isBetween(10L, 99L));
        assertThat(random.longs(500L, 10L, 100L)).hasSize(500);

        // Every element must still be drawn on the owner thread, otherwise reproducibility breaks silently.
        var executor = Executors.newSingleThreadExecutor();
        try {
            var task = executor.submit(() -> random.ints(10, 100).limit(1).sum());
            assertThatThrownBy(task::get).hasCauseInstanceOf(IllegalStateException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void delegatingGeneratorUsesLxm() {
        var random = new DelegatingSplittableRandomGenerator(0L);
        var delegate = ((BoundedSplittableGenerator) random.getDelegate()).delegate();
        assertThat(delegate.getClass().getSimpleName()).isEqualTo("L64X128MixRandom");
    }

}
