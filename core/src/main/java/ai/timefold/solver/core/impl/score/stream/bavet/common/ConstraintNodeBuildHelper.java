package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class ConstraintNodeBuildHelper<Solution_, Score_ extends Score<Score_>>
        extends AbstractNodeBuildHelper<BavetAbstractConstraintStream<Solution_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;

    public ConstraintNodeBuildHelper(Set<BavetAbstractConstraintStream<Solution_>> activeStreamSet,
            AbstractScoreInliner<Score_> scoreInliner) {
        super(activeStreamSet);
        this.scoreInliner = scoreInliner;
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

}
