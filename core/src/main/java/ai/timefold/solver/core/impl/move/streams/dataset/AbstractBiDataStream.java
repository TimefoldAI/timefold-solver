package ai.timefold.solver.core.impl.move.streams.dataset;

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
        return null;
    }

    @Override
    public <ResultA_, ResultB_> BiDataStream<Solution_, ResultA_, ResultB_> map(BiDataMapper<Solution_, A, B, ResultA_> mappingA, BiDataMapper<Solution_, A, B, ResultB_> mappingB) {
        return null;
    }

    @Override
    public BiDataStream<Solution_, A, B> distinct() {
        return null;
    }

    public BiDataset<Solution_, A, B> createDataset() {
        var stream = shareAndAddChild(new TerminalBiDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
