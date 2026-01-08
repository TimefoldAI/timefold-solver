package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.function.BiNeighborhoodsPredicate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UniRightDatasetInstance<Solution_, A, B>
        extends AbstractRightDatasetInstance<Solution_, B> {

    private final IndexerFactory.KeysExtractor<UniTuple<A>> leftCompositeKeyExtractor;
    private final @Nullable BiNeighborhoodsPredicate<Solution_, A, B> filter;

    public UniRightDatasetInstance(AbstractDataset<Solution_> parent, IndexerFactory<B> indexerFactory,
            @Nullable BiNeighborhoodsPredicate<Solution_, A, B> filter, int compositeKeyStoreIndex,
            int rightMostPositionStoreIndex) {
        super(parent, indexerFactory.buildRightKeysExtractor(), compositeKeyStoreIndex, rightMostPositionStoreIndex,
                indexerFactory.buildIndexer(false));
        this.leftCompositeKeyExtractor = indexerFactory.buildUniLeftKeysExtractor();
        this.filter = filter;
    }

    public Object produceCompositeKey(UniTuple<A> leftTuple) {
        return leftCompositeKeyExtractor.apply(leftTuple);
    }

    public @Nullable BiNeighborhoodsPredicate<Solution_, A, B> getFilter() {
        return filter;
    }

}
