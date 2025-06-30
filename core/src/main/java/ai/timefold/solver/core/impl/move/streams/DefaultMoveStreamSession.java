package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetInstance;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSession;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamSession<Solution_>
        implements MoveStreamSession<Solution_>, AutoCloseable {

    private final DatasetSession<Solution_> datasetSession;
    private final SolutionView<Solution_> workingSolutionView;

    public DefaultMoveStreamSession(DatasetSession<Solution_> datasetSession, SolutionView<Solution_> workingSolutionView) {
        this.datasetSession = Objects.requireNonNull(datasetSession);
        this.workingSolutionView = Objects.requireNonNull(workingSolutionView);
    }

    public <Out_ extends AbstractTuple> DatasetInstance<Solution_, Out_>
            getDatasetInstance(AbstractDataset<Solution_, Out_> dataset) {
        return datasetSession.getInstance(dataset);
    }

    public void insert(Object fact) {
        datasetSession.insert(fact);
    }

    public void update(Object fact) {
        datasetSession.update(fact);
    }

    public void retract(Object fact) {
        datasetSession.retract(fact);
    }

    public void settle() {
        datasetSession.settle();
    }

    public SolutionView<Solution_> getWorkingSolutionView() {
        return workingSolutionView;
    }

    @Override
    public void close() {
        datasetSession.close();
    }
}
