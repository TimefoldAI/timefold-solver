package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
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
public abstract class AbstractScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        implements InnerScoreDirectorFactory<Solution_, Score_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final SolutionDescriptor<Solution_> solutionDescriptor;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;

    protected InitializingScoreTrend initializingScoreTrend;

    protected InnerScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory = null;

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

    public InnerScoreDirectorFactory<Solution_, Score_> getAssertionScoreDirectorFactory() {
        return assertionScoreDirectorFactory;
    }

    public void setAssertionScoreDirectorFactory(InnerScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory) {
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
     * In {@link ai.timefold.solver.core.config.solver.EnvironmentMode#TRACKED_FULL_ASSERT},
     * the snapshots are compared when corruption is detected,
     * allowing us to report exactly what variables are different.
     */
    public boolean isTrackingWorkingSolution() {
        return trackingWorkingSolution;
    }

    public void setTrackingWorkingSolution(boolean trackingWorkingSolution) {
        this.trackingWorkingSolution = trackingWorkingSolution;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public InnerScoreDirector<Solution_, Score_> buildScoreDirector() {
        return buildScoreDirector(true, true);
    }

    @Override
    public void assertScoreFromScratch(Solution_ solution) {
        // Get the score before uncorruptedScoreDirector.calculateScore() modifies it
        Score_ score = getSolutionDescriptor().getScore(solution);
        try (var uncorruptedScoreDirector = buildDerivedScoreDirector(false, true)) {
            uncorruptedScoreDirector.setWorkingSolution(solution);
            Score_ uncorruptedScore = uncorruptedScoreDirector.calculateScore();
            if (!score.equals(uncorruptedScore)) {
                throw new IllegalStateException(
                        "Score corruption (" + score.subtract(uncorruptedScore).toShortString()
                                + "): the solution's score (" + score + ") is not the uncorruptedScore ("
                                + uncorruptedScore + ").");
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
