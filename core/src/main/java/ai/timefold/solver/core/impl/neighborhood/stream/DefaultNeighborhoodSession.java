package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhoodSession<Solution_>
        implements NeighborhoodSession {

    private final DatasetSession<Solution_> datasetSession;
    private final SolutionView<Solution_> solutionView;

    public DefaultNeighborhoodSession(DatasetSession<Solution_> datasetSession, SolutionView<Solution_> solutionView) {
        this.datasetSession = Objects.requireNonNull(datasetSession);
        this.solutionView = Objects.requireNonNull(solutionView);
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

    public SolutionView<Solution_> getSolutionView() {
        return solutionView;
    }

}
