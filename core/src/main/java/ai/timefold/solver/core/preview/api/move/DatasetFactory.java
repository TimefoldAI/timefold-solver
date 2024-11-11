package ai.timefold.solver.core.preview.api.move;

import java.util.Collection;

import org.jspecify.annotations.NonNull;

public interface DatasetFactory<Solution_> {

    /**
     * Create a cached stream of all possible values of a class.
     * Read from the planning solution.
     *
     * @param clz
     * @return
     * @param <A>
     */
    <A> @NonNull UniDataStream<Solution_, A> forEach(@NonNull Class<A> clz);

    /**
     * Create a cached stream of arbitrary values.
     * Read from the planning solution.
     *
     * @param clz
     * @param extractor
     * @param <A>
     * @return
     */
    <A> @NonNull UniDataStream<Solution_, A> forEach(@NonNull Class<A> clz, @NonNull SolutionExtractor<Solution_, A> extractor);

    /**
     * Create a cached stream of arbitrary values.
     *
     * @param clz
     * @param collection
     * @param <A>
     * @return
     */
    <A> @NonNull UniDataStream<Solution_, A> forEach(@NonNull Class<A> clz, @NonNull Collection<A> collection);

}
