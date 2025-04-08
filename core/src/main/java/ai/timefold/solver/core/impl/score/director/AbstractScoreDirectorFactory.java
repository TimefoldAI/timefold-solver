package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see ScoreDirectorFactory
 */
public abstract class AbstractScoreDirectorFactory<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
        implements ScoreDirectorFactory<Solution_, Score_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final SolutionDescriptor<Solution_> solutionDescriptor;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;

    protected InitializingScoreTrend initializingScoreTrend;

    protected ScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory = null;

    protected boolean assertClonedSolution = false;
    protected boolean trackingWorkingSolution = false;

    public AbstractScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
        this.listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public ScoreDefinition<Score_> getScoreDefinition() {
        return solutionDescriptor.getScoreDefinition();
    }

    @Override
    public InitializingScoreTrend getInitializingScoreTrend() {
        return initializingScoreTrend;
    }

    public void setInitializingScoreTrend(InitializingScoreTrend initializingScoreTrend) {
        this.initializingScoreTrend = initializingScoreTrend;
    }

    public ScoreDirectorFactory<Solution_, Score_> getAssertionScoreDirectorFactory() {
        return assertionScoreDirectorFactory;
    }

    public void setAssertionScoreDirectorFactory(ScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory) {
        this.assertionScoreDirectorFactory = assertionScoreDirectorFactory;
    }

    public boolean isAssertClonedSolution() {
        return assertClonedSolution;
    }

    public void setAssertClonedSolution(boolean assertClonedSolution) {
        this.assertClonedSolution = assertClonedSolution;
    }

    /**
     * When true, a snapshot of the solution is created before, after and after the undo of a move.
     * In {@link EnvironmentMode#TRACKED_FULL_ASSERT},
     * the snapshots are compared when corruption is detected,
     * allowing us to report exactly what variables are different.
     */
    public boolean isTrackingWorkingSolution() {
        return trackingWorkingSolution;
    }

    public void setTrackingWorkingSolution(boolean trackingWorkingSolution) {
        this.trackingWorkingSolution = trackingWorkingSolution;
    }

    @Override
    public void assertScoreFromScratch(Solution_ solution) {
        // Get the score before uncorruptedScoreDirector.calculateScore() modifies it
        var score = getSolutionDescriptor().<Score_> getScore(solution);
        // Most score directors don't need derived status; CS will override this.
        try (var uncorruptedScoreDirector = createScoreDirectorBuilder()
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .buildDerived()) {
            uncorruptedScoreDirector.setWorkingSolution(solution);
            var uncorruptedScore = uncorruptedScoreDirector.calculateScore()
                    .raw();
            if (!score.equals(uncorruptedScore)) {
                throw new IllegalStateException(
                        "Score corruption (%s): the solution's score (%s) is not the uncorruptedScore (%s)."
                                .formatted(score.subtract(uncorruptedScore).toShortString(), score, uncorruptedScore));
            }
        }
    }

    public void validateEntity(ScoreDirector<Solution_> scoreDirector, Object entity) {
        if (listVariableDescriptor == null) { // Only basic variables.
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isMovable(scoreDirector.getWorkingSolution(), entity)) {
                return;
            }
            for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
                var basicVariableDescriptor = (BasicVariableDescriptor<Solution_>) variableDescriptor;
                if (basicVariableDescriptor.allowsUnassigned()) {
                    continue;
                }
                var value = basicVariableDescriptor.getValue(entity);
                if (value == null) {
                    throw new IllegalStateException(
                            "The entity (%s) has a variable (%s) pinned to null, even though unassigned values are not allowed."
                                    .formatted(entity, basicVariableDescriptor.getVariableName()));
                }
            }
        }
    }

}
