package ai.timefold.solver.test.impl.score.stream;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;

public abstract class AbstractConstraintAssertion<Solution_, Score_ extends Score<Score_>> {

    private boolean initialized = false;
    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory;

    protected AbstractConstraintAssertion(
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory) {
        this.scoreDirectorFactory = scoreDirectorFactory;
    }

    abstract Solution_ getSolution();

    abstract void update(InnerScore<Score_> score, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap);

    /**
     * The logic ensures the solution is initialized only once.
     * This is necessary because settingAllShadowVariables can also initialize the score and constraint data.
     * Therefore, we might miss listener events if we call the initialization steps for an already initialized solution.
     */
    void ensureInitialized() {
        if (initialized) {
            return;
        }
        // Most score directors don't need derived status; CS will override this.
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .buildDerived()) {
            scoreDirector.setWorkingSolution(getSolution());
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
            if (scoreDirector instanceof BavetConstraintStreamScoreDirector<?, ?> bavetConstraintStreamScoreDirector) {
                bavetConstraintStreamScoreDirector.clearShadowVariablesListenerQueue();
            }
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                    scoreDirector.getIndictmentMap());
            initialized = true;
        }
    }

    void toggleInitialized() {
        this.initialized = true;
    }
}
