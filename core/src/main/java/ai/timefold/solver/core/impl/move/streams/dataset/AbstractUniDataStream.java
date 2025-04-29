package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.impl.bavet.bi.joiner.BiJoinerComber;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.ForeBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractUniDataStream<Solution_, A> extends AbstractDataStream<Solution_>
        implements UniDataStream<Solution_, A> {

    protected AbstractUniDataStream(DataStreamFactory<Solution_> dataStreamFactory) {
        super(dataStreamFactory, null);
    }

    protected AbstractUniDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            @Nullable AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public final UniDataStream<Solution_, A> filter(Predicate<A> predicate) {
        return shareAndAddChild(new FilterUniDataStream<>(dataStreamFactory, this, predicate));
    }

    @SafeVarargs
    @Override
    public final <B> UniDataStream<Solution_, A> ifExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return ifExists(dataStreamFactory.forEachNonDiscriminating(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniDataStream<Solution_, A> ifExists(UniDataStream<Solution_, B> otherStream, BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniDataStream<Solution_, A> ifNotExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, dataStreamFactory.forEachNonDiscriminating(otherClass), joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniDataStream<Solution_, A> ifNotExists(UniDataStream<Solution_, B> otherStream,
            BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <B> UniDataStream<Solution_, A> ifExistsOrNot(boolean shouldExist, UniDataStream<Solution_, B> otherStream,
            BiJoiner<A, B>[] joiners) {
        var other = (AbstractUniDataStream<Solution_, B>) otherStream;
        var joinerComber = BiJoinerComber.comb(joiners);
        var parentBridgeB = other.shareAndAddChild(new ForeBridgeUniDataStream<Solution_, B>(dataStreamFactory, other));
        return dataStreamFactory.share(new IfExistsUniDataStream<>(dataStreamFactory, this, parentBridgeB, shouldExist,
                joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()), childStreamList::add);
    }

    public UniDataset<Solution_, A> createDataset() {
        var stream = shareAndAddChild(new TerminalUniDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
