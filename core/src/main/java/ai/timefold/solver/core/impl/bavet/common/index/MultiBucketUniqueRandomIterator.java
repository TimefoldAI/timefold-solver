package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.solver.random.RandomUtils;

import org.jspecify.annotations.NullMarked;

/**
 * Picks uniformly at random, without replacement, across several disjoint buckets,
 * each already weighted by its own size.
 * Unlike a boundary-ordered walk that drains one bucket before moving to the next ({@code ComparisonIndexer.DefaultIterator},
 * {@code ContainedInIndexer.DefaultIterator}),
 * every {@link #next()} here re-samples a bucket from the {@link #distribution weights that remain},
 * so the first draw (and every later one) is not biased towards whichever bucket happens to be walked first.
 * <p>
 * {@link #distribution} holds each bucket's remaining, not original, size:
 * {@link #next()} decrements the chosen bucket's weight (and {@link #distributionSum}) after drawing from it,
 * so an exhausted bucket can no longer be chosen.
 * This assumes the buckets are disjoint
 * (no tuple is reachable through more than one bucket).
 * The case where they are not ({@code ContainingAnyOfIndexer})
 * cannot use this class at all;
 * no bucket weighting can make it uniform.
 * <p>
 * Building this walks every matching bucket upfront to learn its size,
 * which is why {@code randomIterator} (the repeating, with-replacement flavor) stays the cheap default instead.
 *
 * @param <T>
 */
@NullMarked
final class MultiBucketUniqueRandomIterator<T> implements UniqueRandomIterator<T> {

    private final List<UniqueRandomIterator<T>> bucketIteratorList;
    private final int[] distribution;
    private final RandomGenerator workingRandom;

    private int distributionSum;

    MultiBucketUniqueRandomIterator(List<UniqueRandomIterator<T>> bucketIteratorList, int[] distribution,
            RandomGenerator workingRandom) {
        this.bucketIteratorList = bucketIteratorList;
        this.distribution = distribution;
        this.workingRandom = workingRandom;
        var sum = 0;
        for (var weight : distribution) {
            sum += weight;
        }
        this.distributionSum = sum;
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
        distribution[selectedIndex]--;
        distributionSum--;
        return bucketIteratorList.get(selectedIndex).next();
    }

}
