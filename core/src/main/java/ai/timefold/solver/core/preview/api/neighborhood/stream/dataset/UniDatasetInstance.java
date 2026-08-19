package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link UniDataset}, resolved against a specific solving session.
 *
 * @param <A> the type of the dataset's rows
 */
@NullMarked
public interface UniDatasetInstance<A> {

    /**
     * Returns a theoretical upper bound on the number of rows;
     * {@code filtering()} joiners are not accounted for, so the iterator may yield fewer.
     * This method can be very expensive,
     * as it may require traversing the entire dataset.
     */
    int size();

    /**
     * Returns an iterator performing sampling with replacement:
     * a random walk over the dataset, never an indexed ({@code get(i)}) random access.
     * Never ends, and may return the same row more than once;
     * {@link Iterator#remove()} is not supported.
     * <p>
     * This is the cheap default; prefer it over {@link #exhaustiveIterator(RandomGenerator)}
     * unless the caller specifically needs every row exactly once.
     *
     * @param random never null
     * @return never null
     */
    Iterator<@Nullable A> iterator(RandomGenerator random);

    /**
     * As defined by {@link #iterator(RandomGenerator)},
     * but performing sampling without replacement:
     * every row is eventually returned exactly once,
     * and then the iterator ends, without any cooperation from the caller.
     * {@link Iterator#remove()} is not supported and never needs to be called;
     * the caller cannot break uniqueness.
     * Significantly more expensive to create and maintain than {@link #iterator(RandomGenerator)},
     * to the point where large datasets may become impractical in terms of memory and CPU,
     * especially in the case of large multi-leveled joins.
     *
     * @param random never null
     * @return never null
     */
    Iterator<@Nullable A> exhaustiveIterator(RandomGenerator random);

}
