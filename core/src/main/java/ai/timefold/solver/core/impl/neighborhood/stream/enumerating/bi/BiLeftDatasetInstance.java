package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RepeatingRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Adds a per-A index on top of the plain flat tuple list {@link AbstractLeftDatasetInstance} already maintains,
 * so {@code CachedBiDatasetInstance}'s per-A queries do not need to scan every materialized pair and reject non-matches,
 * and therefore need no bail-out at all.
 */
@NullMarked
public final class BiLeftDatasetInstance<Solution_, A, B>
        extends AbstractLeftDatasetInstance<Solution_, BiTuple<A, B>> {

    /**
     * Built again after each change of the dataset, in {@link #indexByA()}; it is not incremental.
     * It does not need to be: the dataset only changes in {@code NeighborhoodSession.settle()},
     * which the solver calls at the start and at the end of a step, but never during move iteration.
     * One step therefore does one rebuild at the most, and all queries of that step share it.
     * <p>
     * Can easily be made incremental,
     * if profiling shows the rebuild as a significant issue.
     */
    private @Nullable Map<@Nullable A, ElementAwareArrayList<BiTuple<A, B>>> indexByA;

    public BiLeftDatasetInstance(AbstractDataset<Solution_> parent, int entryStoreIndex) {
        super(parent, entryStoreIndex);
    }

    @Override
    public void insert(BiTuple<A, B> tuple) {
        super.insert(tuple);
        indexByA = null;
    }

    @Override
    public void update(BiTuple<A, B> tuple) {
        super.update(tuple);
        indexByA = null;
    }

    @Override
    public void retract(BiTuple<A, B> tuple) {
        super.retract(tuple);
        indexByA = null;
    }

    private Map<@Nullable A, ElementAwareArrayList<BiTuple<A, B>>> indexByA() {
        if (indexByA == null) {
            var freshIndexByA = new HashMap<@Nullable A, ElementAwareArrayList<BiTuple<A, B>>>();
            for (var tuple : this) {
                freshIndexByA.computeIfAbsent(tuple.getA(), ignored -> new ElementAwareArrayList<>()).add(tuple);
            }
            indexByA = freshIndexByA;
        }
        return indexByA;
    }

    private ElementAwareArrayList<BiTuple<A, B>> bucketOrEmpty(@Nullable A a) {
        var bucket = indexByA().get(a);
        return bucket != null ? bucket : new ElementAwareArrayList<>();
    }

    /**
     * As defined by {@link #size()}, restricted to tuples paired with the given left value.
     */
    public int size(@Nullable A a) {
        return bucketOrEmpty(a).size();
    }

    /**
     * As defined by {@link #iterator(RandomGenerator)}, restricted to tuples paired with the given left
     * value. No filter, no bail-out needed: {@link #bucketOrEmpty} is already exactly that restriction.
     */
    public RepeatingRandomIterator<BiTuple<A, B>> randomIterator(@Nullable A a, RandomGenerator workingRandom) {
        return RepeatingRandomIterator.of(bucketOrEmpty(a), workingRandom);
    }

    /**
     * As defined by {@link #exhaustiveIterator(RandomGenerator)}, restricted to tuples paired with the
     * given left value.
     */
    public UniqueRandomIterator<BiTuple<A, B>> exhaustiveRandomIterator(@Nullable A a, RandomGenerator workingRandom) {
        return UniqueRandomIterator.of(bucketOrEmpty(a), workingRandom);
    }

}
