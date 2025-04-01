package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.MultiConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.MultiConstraintListener;

public final class DefaultMultiConstraintListener<Solution_, Score_ extends Score<Score_>>
        extends AbstractMultiConstraintAssertion<Score_> implements MultiConstraintListener {

    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory;
    private final Solution_ solution;
    private boolean initialized = false;

    DefaultMultiConstraintListener(ConstraintProvider constraintProvider,
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory,
            Solution_ solution) {
        super(constraintProvider);
        this.scoreDirectorFactory = requireNonNull(scoreDirectorFactory);
        this.solution = Objects.requireNonNull(solution);
    }

    @Override
    void ensureInitialized() {
        if (!initialized) {
            // Most score directors don't need derived status; CS will override this.
            try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                    .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .buildDerived()) {
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
    public MultiConstraintAssertion settingAllShadowVariables() {
        // Most score directors don't need derived status; CS will override this.
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .buildDerived()) {
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                    scoreDirector.getIndictmentMap());
            initialized = true;
            return this;
        }
    }
}
