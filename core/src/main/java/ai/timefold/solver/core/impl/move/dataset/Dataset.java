package ai.timefold.solver.core.impl.move.dataset;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;

public final class Dataset<Solution_> {

    private final DefaultDatasetFactory<Solution_> defaultDatasetFactory;
    private final BavetScoringConstraintStream<Solution_> scoringConstraintStream;

    public Dataset(DefaultDatasetFactory<Solution_> defaultDatasetFactory,
            BavetScoringConstraintStream<Solution_> scoringConstraintStream) {
        this.defaultDatasetFactory = Objects.requireNonNull(defaultDatasetFactory);
        this.scoringConstraintStream = Objects.requireNonNull(scoringConstraintStream);
    }

    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        // scoringConstraintStream.collectActiveDataStreams(dataStreamSet);
    }

}
