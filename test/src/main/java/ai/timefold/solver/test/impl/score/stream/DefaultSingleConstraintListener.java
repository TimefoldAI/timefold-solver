package ai.timefold.solver.test.impl.score.stream;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.SingleConstraintListener;

public final class DefaultSingleConstraintListener<Solution_, Score_ extends Score<Score_>>
        extends AbstractSingleConstraintAssertion<Solution_, Score_> implements SingleConstraintListener {

    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;
    private final Solution_ solution;
    private boolean initialized = false;

    DefaultSingleConstraintListener(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
                                    Solution_ solution) {
        super(scoreDirectorFactory);
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.solution = Objects.requireNonNull(solution);
    }

    /**
     * The logic ensures the solution is initialized only once.
     * This is necessary because settingAllShadowVariables can also initialize the score and constraint data.
     * Therefore, we might miss listener events if we call the initialization steps for an already initialized solution.
     */
    @Override
    void ensureInitialized() {
        if (!initialized) {
            try (var scoreDirector = scoreDirectorFactory.buildDerivedScoreDirector(true, ConstraintMatchPolicy.ENABLED)) {
                scoreDirector.setWorkingSolution(solution);
                // When models include custom listeners,
                // the notification queue may no longer be empty
                // because the shadow variable might be linked to a source
                // that has changed during the solution initialization.
                // As a result,
                // any validation using a solution would never work in these cases
                // due to an error when calling calculateScore().
                // Calling scoreDirector.triggerVariableListeners() runs the custom listeners and clears the queue.
                // However, to maintain API consistency,
                // we will only trigger the listeners
                // if the user opts to use settingAllShadowVariables.
                scoreDirector.clearVariableListenerEvents();
                update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                        scoreDirector.getIndictmentMap());
                initialized = true;
            }
        }
    }

    @Override
    public SingleConstraintAssertion settingAllShadowVariables() {
        try (var scoreDirector = scoreDirectorFactory.buildDerivedScoreDirector(true, ConstraintMatchPolicy.ENABLED)) {
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                    scoreDirector.getIndictmentMap());
            initialized = true;
            return this;
        }
    }

}
