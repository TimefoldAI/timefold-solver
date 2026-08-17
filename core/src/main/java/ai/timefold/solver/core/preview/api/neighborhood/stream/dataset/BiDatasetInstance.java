package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link BiDataset}, resolved against a specific solving session.
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
     * As defined by {@link UniDatasetInstance#iterator(RandomGenerator)}.
     */
    BiIterator<A, B> iterator(RandomGenerator random);

    /**
     * As defined by {@link UniDatasetInstance#exhaustiveIterator(RandomGenerator)}.
     */
    BiIterator<A, B> exhaustiveIterator(RandomGenerator random);

    /**
     * As defined by {@link #size()},
     * but restricted to rows paired with the given left value.
     */
    int size(@Nullable A a);

    /**
     * As defined by {@link #iterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> iterator(@Nullable A a, RandomGenerator random);

    /**
     * As defined by {@link #exhaustiveIterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> exhaustiveIterator(@Nullable A a, RandomGenerator random);

}
