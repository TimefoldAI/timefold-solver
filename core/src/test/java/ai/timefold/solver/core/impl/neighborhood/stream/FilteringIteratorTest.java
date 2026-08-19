package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

class FilteringIteratorTest {

    @Test
    void noBailOut_behavesLikeAFilteredFiniteIterator() {
        var delegate = List.of(1, 2, 3, 4, 5).iterator();
        var iterator = new FilteringIterator<>(delegate, i -> i % 2 == 0);

        assertThat(iterator).toIterable().containsExactly(2, 4);
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

    @Test
    void bailOut_rejectingFilterOnNeverEndingDelegateStillTerminates() {
        var neverEnding = new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return 0; // Always rejected by the filter below.
            }
        };
        var iterator = new FilteringIterator<>(neverEnding, i -> false, 1_000);

        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void bailOut_acceptingFilterStillFindsItsMatch() {
        var random = new Random(0);
        var repeating = new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return random.nextInt(1_000);
            }
        };
        var iterator = new FilteringIterator<>(repeating, i -> i == 0 || i == 1, 10_000);

        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isIn(0, 1);
    }

    /**
     * The bail-out budget is a local, reset on every {@link FilteringIterator#hasNext()} call, not an
     * instance-level counter: a bail-out is a per-call false negative, not proof that no match exists.
     */
    @Test
    void bailOut_resetsOnEveryHasNextCall() {
        var nextValue = new int[] { 0 };
        var counting = new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return nextValue[0]++;
            }
        };
        var iterator = new FilteringIterator<>(counting, i -> i >= 1_000, 1_000);

        // The first call spends its whole budget on candidates 0..999 (all rejected) and bails out.
        assertThat(iterator.hasNext()).isFalse();
        // A later call gets a fresh budget and immediately finds the match the first call never reached.
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(1_000);
    }

}
