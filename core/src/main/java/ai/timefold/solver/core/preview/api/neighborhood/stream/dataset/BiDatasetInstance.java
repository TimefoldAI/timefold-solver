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
     * As defined by {@link UniDatasetInstance#size()}.
     */
    int size();

    /**
     * As defined by {@link UniDatasetInstance#iterator()}.
     */
    BiIterator<A, B> iterator();

    /**
     * As defined by {@link UniDatasetInstance#randomIterator(RandomGenerator)}.
     */
    BiIterator<A, B> randomIterator(RandomGenerator random);

    /**
     * As defined by {@link UniDatasetInstance#uniqueRandomIterator(RandomGenerator)}.
     */
    BiIterator<A, B> uniqueRandomIterator(RandomGenerator random);

    /**
     * As defined by {@link #size()},
     * but restricted to rows paired with the given left value.
     */
    int size(@Nullable A a);

    /**
     * As defined by {@link #iterator()},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> iterator(@Nullable A a);

    /**
     * As defined by {@link #randomIterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random);

    /**
     * As defined by {@link #uniqueRandomIterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> uniqueRandomIterator(@Nullable A a, RandomGenerator random);

}
