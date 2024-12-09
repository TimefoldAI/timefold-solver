package ai.timefold.solver.core.impl.move.dataset;

import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.impl.bavet.common.index.joiner.BiJoinerComber;
import ai.timefold.solver.core.impl.move.dataset.common.bridge.ForeBridgeUniDataStream;
import ai.timefold.solver.core.preview.api.move.Dataset;
import ai.timefold.solver.core.preview.api.move.UniDataStream;

import org.jspecify.annotations.NonNull;

public abstract class AbstractUniDataStream<Solution_, A> extends AbstractDataStream<Solution_>
        implements UniDataStream<Solution_, A> {

    protected AbstractUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory) {
        super(datasetFactory, null);
    }

    protected AbstractUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory, AbstractDataStream<Solution_> parent) {
        super(datasetFactory, parent);
    }

    @Override
    public final @NonNull UniDataStream<Solution_, A> filter(@NonNull Predicate<A> predicate) {
        return shareAndAddChild(new FilterUniDataStream<>(datasetFactory, this, predicate));
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifExists(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExists(datasetFactory.forEach(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifExists(@NonNull UniDataStream<Solution_, B> otherStream,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExists(datasetFactory.forEachIncludingUnassigned(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifNotExists(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, datasetFactory.forEach(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifNotExists(@NonNull UniDataStream<Solution_, B> otherStream,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifNotExists(datasetFactory.forEachIncludingUnassigned(otherClass), joiners);
    }

    private <B> UniDataStream<Solution_, A> ifExistsOrNot(boolean shouldExist, UniDataStream<Solution_, B> otherStream,
            BiJoiner<A, B>[] joiners) {
        var other = (AbstractUniDataStream<Solution_, B>) otherStream;
        var joinerComber = BiJoinerComber.comb(joiners);
        var parentBridgeB = other.shareAndAddChild(new ForeBridgeUniDataStream<Solution_, B>(datasetFactory, other));
        return datasetFactory.share(new IfExistsUniDataStream<>(datasetFactory, this, parentBridgeB, shouldExist,
                joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()), childStreamList::add);
    }

    @Override
    public Dataset<Solution_, Object> createDataset() {
        return null;
    }

}
