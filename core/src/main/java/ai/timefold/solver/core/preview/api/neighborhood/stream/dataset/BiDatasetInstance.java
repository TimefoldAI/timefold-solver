package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link BiDataset}, resolved against a specific solving session.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 *
 * @param <A> the type of the dataset's left rows
 * @param <B> the type of the dataset's right rows
 */
@NullMarked
public interface BiDatasetInstance<A, B> {

    /**
     * Returns a theoretical upper bound on the number of rows;
     * {@code filtering()} joiners are not accounted for, so the iterator may yield fewer.
     */
    int size();

    BiIterator<A, B> iterator();

    /**
     * Returns an iterator performing sampling without replacement:
     * a random walk over the dataset, never an indexed ({@code get(i)}) random access.
     *
     * @param random never null
     * @return never null
     */
    BiIterator<A, B> randomIterator(RandomGenerator random);

    /**
     * Returns a theoretical upper bound on the number of rows paired with the given left value;
     * {@code filtering()} joiners are not accounted for, so the iterator may yield fewer.
     */
    int size(@Nullable A a);

    Iterator<@Nullable B> iterator(@Nullable A a);

    /**
     * As defined by {@link #randomIterator(RandomGenerator)}, but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random);

}
