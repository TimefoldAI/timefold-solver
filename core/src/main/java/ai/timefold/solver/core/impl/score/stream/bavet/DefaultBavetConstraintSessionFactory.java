package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public class DefaultBavetConstraintSessionFactory<Solution_, Score_ extends Score<Score_>>
        extends
        AbstractBavetConstraintSessionFactory<Solution_, Score_, DefaultBavetConstraintSessionFactory.SessionData<Solution_>, BavetConstraintSession<Score_>> {
    protected record SessionData<Solution_>(SequencedSet<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
    }

    public DefaultBavetConstraintSessionFactory(
            SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintMetaModel constraintMetaModel, boolean profilingEnabled) {
        super(solutionDescriptor, constraintMetaModel, profilingEnabled);
    }

    @Override
    protected SessionData<Solution_> createSessionData(Solution_ solution, ScoreDefinition<Score_> scoreDefinition) {
        return new SessionData<>(new LinkedHashSet<>());
    }

    @Override
    protected void addConstraintToSession(BavetConstraint<Solution_> constraint, Score_ constraintWeight,
            SessionData<Solution_> sessionData) {
        /*
         * Relies on BavetConstraintFactory#share(Stream_) occurring for all constraint stream instances
         * to ensure there are no 2 equal ConstraintStream instances (with different child stream lists).
         */
        constraint.collectActiveConstraintStreams(sessionData.constraintStreamSet);
    }

    @Override
    protected BavetConstraintSession<Score_> createSessionFromData(SessionData<Solution_> sessionData,
            Solution_ workingSolution,
            ConsistencyTracker<Solution_> consistencyTracker,
            ConstraintMatchPolicy constraintMatchPolicy,
            boolean scoreDirectorDerived,
            Map<Constraint, Score_> constraintWeightMap,
            ScoreDefinition<Score_> scoreDefinition,
            Score_ zeroScore) {
        var scoreInliner = AbstractScoreInliner.buildScoreInliner(scoreDefinition, constraintWeightMap, constraintMatchPolicy);
        if (sessionData.constraintStreamSet.isEmpty()) {
            return new BavetConstraintSession<>(scoreInliner);
        }

        return new BavetConstraintSession<>(scoreInliner,
                buildNodeNetwork(workingSolution, consistencyTracker, sessionData.constraintStreamSet,
                        scoreInliner, constraintProfiler, constraintMatchPolicy.isIndictmentsEnabled(),
                        scoreDirectorDerived));
    }

}
