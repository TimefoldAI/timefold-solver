package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.PropagationQueue;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

/**
 * The type is public to make it easier for Bavet-specific minimal bug reproducers to be created.
 * Instances should be created through
 * {@link BavetConstraintStreamScoreDirectorFactory#newSession(Object, ConstraintMatchPolicy, boolean)}.
 *
 * @see PropagationQueue Description of the tuple propagation mechanism.
 *
 * @param <Score_>
 */
public final class BavetConstraintSession<Score_ extends Score<Score_>> extends AbstractSession {

    private final AbstractScoreInliner<Score_> scoreInliner;

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner) {
        this(scoreInliner, NodeNetwork.EMPTY);
    }

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner, NodeNetwork nodeNetwork) {
        super(nodeNetwork);
        this.scoreInliner = scoreInliner;
    }

    public Score_ calculateScore() {
        settle();
        return scoreInliner.extractScore();
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return scoreInliner.getConstraintIdToConstraintMatchTotalMap();
    }

    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return scoreInliner.getIndictmentMap();
    }

}
