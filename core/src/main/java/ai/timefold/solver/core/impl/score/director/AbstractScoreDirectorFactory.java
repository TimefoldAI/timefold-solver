package ai.timefold.solver.core.impl.score.director;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see ScoreDirectorFactory
 */
@NullMarked
public abstract class AbstractScoreDirectorFactory<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
        implements ScoreDirectorFactory<Solution_, Score_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final SolutionDescriptor<Solution_> solutionDescriptor;
    protected final EnvironmentMode globalEnvironmentMode;
    @Nullable
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;
    @Nullable
    protected InitializingScoreTrend initializingScoreTrend;
    @Nullable
    protected ScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory = null;

    protected AbstractScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            EnvironmentMode globalEnvironmentMode) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        this.globalEnvironmentMode = globalEnvironmentMode;
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
    public @Nullable InitializingScoreTrend getInitializingScoreTrend() {
        return initializingScoreTrend;
    }

    @Override
    public AbstractScoreDirector.AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
        return createScoreDirectorBuilder(globalEnvironmentMode);
    }

    public void setInitializingScoreTrend(InitializingScoreTrend initializingScoreTrend) {
        this.initializingScoreTrend = initializingScoreTrend;
    }

    public @Nullable ScoreDirectorFactory<Solution_, Score_> getAssertionScoreDirectorFactory() {
        return assertionScoreDirectorFactory;
    }

    public void setAssertionScoreDirectorFactory(ScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory) {
        this.assertionScoreDirectorFactory = assertionScoreDirectorFactory;
    }

    public EntityDescriptor<Solution_> validateEntity(ScoreDirector<Solution_> scoreDirector, Object entity) {
        if (listVariableDescriptor == null) { // Only basic variables.
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isMovable(scoreDirector.getWorkingSolution(), entity)) {
                return entityDescriptor;
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
            return entityDescriptor;
        }
        return listVariableDescriptor.getEntityDescriptor();
    }

}
