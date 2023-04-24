package ai.timefold.solver.constraint.streams.common;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;

public abstract class AbstractConstraintStreamScoreDirectorFactoryService<Solution_, Score_ extends Score<Score_>>
        implements ScoreDirectorFactoryService<Solution_, Score_> {

    public abstract boolean supportsImplType(ConstraintStreamImplType constraintStreamImplType);

    public abstract AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory(
            SolutionDescriptor<Solution_> solutionDescriptor, ConstraintProvider constraintProvider,
            EnvironmentMode environmentMode);

    public abstract ConstraintFactory buildConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            EnvironmentMode environmentMode);

}
