package ai.timefold.solver.core.impl.move.streams.dataset.bi;

import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeBiDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataMapper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractBiDataStream<Solution_, A, B> extends AbstractDataStream<Solution_>
        implements BiDataStream<Solution_, A, B> {

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory) {
        super(dataStreamFactory, null);
    }

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            @Nullable AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public final BiDataStream<Solution_, A, B> filter(BiDataFilter<Solution_, A, B> filter) {
        return shareAndAddChild(new FilterBiDataStream<>(dataStreamFactory, this, filter));
    }

    @Override
    public <ResultA_> UniDataStream<Solution_, ResultA_> map(BiDataMapper<Solution_, A, B, ResultA_> mapping) {
        var stream = shareAndAddChild(new UniMapBiDataStream<>(dataStreamFactory, this, mapping));
        return dataStreamFactory.share(new AftBridgeUniDataStream<>(dataStreamFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiDataStream<Solution_, ResultA_, ResultB_> map(BiDataMapper<Solution_, A, B, ResultA_> mappingA, BiDataMapper<Solution_, A, B, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BiMapBiDataStream<>(dataStreamFactory, this, mappingA, mappingB));
        return dataStreamFactory.share(new AftBridgeBiDataStream<>(dataStreamFactory, stream), stream::setAftBridge);
    }

    @Override
    public BiDataStream<Solution_, A, B> distinct() {
        if (guaranteesDistinct()) {
            return this; // Already distinct, no need to create a new stream.
        }
        throw new UnsupportedOperationException();
    }

    public BiDataset<Solution_, A, B> createDataset() {
        var stream = shareAndAddChild(new TerminalBiDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
