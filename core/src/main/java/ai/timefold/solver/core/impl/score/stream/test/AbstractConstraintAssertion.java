package ai.timefold.solver.core.impl.score.stream.test;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchTotal;
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

    abstract void update(InnerScore<Score_> score, Map<ConstraintRef, ConstraintMatchTotal<Score_>> constraintMatchTotalMap);

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
            // Users use settingAllShadowVariables to set shadow variables
            var solution = getSolution();
            BavetConstraintStreamScoreDirector<Solution_, Score_> bavetConstraintStreamScoreDirector = null;

            if (scoreDirector instanceof BavetConstraintStreamScoreDirector) {
                bavetConstraintStreamScoreDirector = (BavetConstraintStreamScoreDirector<Solution_, Score_>) scoreDirector;
            }

            if (bavetConstraintStreamScoreDirector != null) {
                bavetConstraintStreamScoreDirector.updateConsistencyFromSolution(solution);
            }
            scoreDirector.setWorkingSolutionWithoutUpdatingShadows(solution);
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
            if (bavetConstraintStreamScoreDirector != null) {
                bavetConstraintStreamScoreDirector.clearShadowVariablesListenerQueue();
            }
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap());
            initialized = true;
        }
    }

    void toggleInitialized() {
        this.initialized = true;
    }

    protected String explainScore(InnerScore<Score_> workingScore,
            Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection) {
        return TimefoldSolverEnterpriseService.loadOrDefault(
                s -> {
                    var constraintAnalyses = new TreeMap<ConstraintRef, ConstraintMatchTotal<Score_>>();
                    for (var constraintMatchTotal : constraintMatchTotalCollection) {
                        var constraintRef = constraintMatchTotal.getConstraintRef();
                        constraintAnalyses.put(constraintRef, constraintMatchTotal);
                    }
                    return s.analyze(workingScore, constraintAnalyses, ScoreAnalysisFetchPolicy.FETCH_ALL)
                            .summarize();
                },
                () -> "Score analysis is only available in Timefold Solver Enterprise Edition.");
    }
}
