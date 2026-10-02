package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.solver.random.RandomUtils;

import org.jspecify.annotations.NullMarked;

/**
 * Every {@link #next()} independently picks a downstream iterator weighted by {@link #distribution},
 * with replacement, so this class never ends:
 * there is no shrinking distribution and no removed set to maintain.
 * <p>
 * Shared by {@link ComparisonIndexer}, {@link ContainedInIndexer} and {@link ContainingAnyOfIndexer},
 * whose only difference is how {@code downstreamIteratorList}/{@code distribution} get built
 * (bucket enumeration strategy, boundary handling, null handling) -- construction stays with each indexer,
 * only the resulting state and the two identical methods below live here.
 *
 * @param <T>
 */
@NullMarked
final class WeightedRepeatingIterator<T> implements RepeatingRandomIterator<T> {

    private final List<Iterator<T>> downstreamIteratorList;
    private final int[] distribution;
    private final int distributionSum;
    private final RandomGenerator workingRandom;

    WeightedRepeatingIterator(List<Iterator<T>> downstreamIteratorList, int[] distribution, int distributionSum,
            RandomGenerator workingRandom) {
        this.downstreamIteratorList = downstreamIteratorList;
        this.distribution = distribution;
        this.distributionSum = distributionSum;
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return distributionSum > 0;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var selectedIndex = RandomUtils.sampleWithDistribution(workingRandom, distributionSum, distribution);
        return downstreamIteratorList.get(selectedIndex).next();
    }

    // No remove()/forEachRemaining() overrides: RepeatingRandomIterator's defaults already throw.

}
