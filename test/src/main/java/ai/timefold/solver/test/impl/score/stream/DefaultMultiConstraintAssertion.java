package ai.timefold.solver.test.impl.score.stream;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;

public final class DefaultMultiConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        extends AbstractMultiConstraintAssertion<Solution_, Score_> {

    DefaultMultiConstraintAssertion(ConstraintProvider constraintProvider,
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory, Score_ actualScore,
            Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        super(constraintProvider, scoreDirectorFactory);
        update(InnerScore.fullyAssigned(actualScore), constraintMatchTotalMap, indictmentMap);
    }

    @Override
    Solution_ getSolution() {
        throw new IllegalStateException("Impossible state as the solution is initialized at the constructor.");
    }
}
