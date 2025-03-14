package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.impl.bavet.bi.joiner.BiJoinerComber;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.ForeBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NonNull;

public abstract class AbstractUniDataStream<Solution_, A> extends AbstractDataStream<Solution_>
        implements UniDataStream<Solution_, A> {

    protected AbstractUniDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory) {
        super(dataStreamFactory, null);
    }

    protected AbstractUniDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory,
            AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public final @NonNull UniDataStream<Solution_, A> filter(@NonNull Predicate<A> predicate) {
        return shareAndAddChild(new FilterUniDataStream<>(dataStreamFactory, this, predicate));
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifExists(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExists(dataStreamFactory.forEach(otherClass), joiners);
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
        return ifExists(dataStreamFactory.forEachIncludingUnassigned(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <B> UniDataStream<Solution_, A> ifNotExists(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, dataStreamFactory.forEach(otherClass), joiners);
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
        return ifNotExists(dataStreamFactory.forEachIncludingUnassigned(otherClass), joiners);
    }

    private <B> UniDataStream<Solution_, A> ifExistsOrNot(boolean shouldExist, UniDataStream<Solution_, B> otherStream,
            BiJoiner<A, B>[] joiners) {
        var other = (AbstractUniDataStream<Solution_, B>) otherStream;
        var joinerComber = BiJoinerComber.comb(joiners);
        var parentBridgeB = other.shareAndAddChild(new ForeBridgeUniDataStream<Solution_, B>(dataStreamFactory, other));
        return dataStreamFactory.share(new IfExistsUniDataStream<>(dataStreamFactory, this, parentBridgeB, shouldExist,
                joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()), childStreamList::add);
    }

    @Override
    public UniDataStream<Solution_, A> addNull() {
        throw new UnsupportedOperationException();
    }

    public AbstractDataset<Solution_, UniTuple<A>> createDataset() {
        var stream = shareAndAddChild(new TerminalUniDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
