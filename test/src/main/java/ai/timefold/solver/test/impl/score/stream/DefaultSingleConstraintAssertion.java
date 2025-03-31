package ai.timefold.solver.test.impl.score.stream;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;

public final class DefaultSingleConstraintAssertion <Solution_, Score_ extends Score<Score_>>
        extends AbstractSingleConstraintAssertion<Solution_, Score_>{

    DefaultSingleConstraintAssertion(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory,
                                     Score_ score, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
                                     Map<Object, Indictment<Score_>> indictmentMap) {
        super(scoreDirectorFactory);
        update(score, constraintMatchTotalMap, indictmentMap);
    }


    @Override
    void ensureInitialized() {
        // There is no need to take any action, as the data has already been initialized in the constructor
    }
}
