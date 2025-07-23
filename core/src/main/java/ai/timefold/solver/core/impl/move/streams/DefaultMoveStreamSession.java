package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.dataset.BiDataset;
import ai.timefold.solver.core.impl.move.streams.dataset.BiDatasetInstance;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSession;
import ai.timefold.solver.core.impl.move.streams.dataset.UniDataset;
import ai.timefold.solver.core.impl.move.streams.dataset.UniDatasetInstance;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamSession<Solution_>
        implements MoveStreamSession<Solution_>, AutoCloseable {

    private final DatasetSession<Solution_> datasetSession;
    private final Solution_ workingSolution;

    public DefaultMoveStreamSession(DatasetSession<Solution_> datasetSession, Solution_ workingSolution) {
        this.datasetSession = Objects.requireNonNull(datasetSession);
        this.workingSolution = Objects.requireNonNull(workingSolution);
    }

    public <A> UniDatasetInstance<Solution_, A> getDatasetInstance(UniDataset<Solution_, A> dataset) {
        return (UniDatasetInstance<Solution_, A>) datasetSession.getInstance(dataset);
    }

    public <A, B> BiDatasetInstance<Solution_, A, B> getDatasetInstance(BiDataset<Solution_, A, B> dataset) {
        return (BiDatasetInstance<Solution_, A, B>) datasetSession.getInstance(dataset);
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

    public Solution_ getWorkingSolution() {
        return workingSolution;
    }

    @Override
    public void close() {
        datasetSession.close();
    }
}
