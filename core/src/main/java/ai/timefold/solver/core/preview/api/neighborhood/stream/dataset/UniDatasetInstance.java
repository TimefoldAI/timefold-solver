package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link UniDataset}, resolved against a specific solving session.
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
 * @param <A> the type of the dataset's rows
 */
@NullMarked
public interface UniDatasetInstance<A> {

    int size();

    Iterator<@Nullable A> iterator();

    /**
     * Returns an iterator performing sampling without replacement:
     * a random walk over the dataset, never an indexed ({@code get(i)}) random access.
     *
     * @param random never null
     * @return never null
     */
    Iterator<@Nullable A> randomIterator(RandomGenerator random);

}
