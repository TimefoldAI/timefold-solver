package ai.timefold.solver.constraint.streams.bavet;

import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class BavetConstraintStreamImplSupport
        implements ConstraintStreamImplSupport {

    private final boolean constraintMatchEnabled;

    public BavetConstraintStreamImplSupport(boolean constraintMatchEnabled) {
        this.constraintMatchEnabled = constraintMatchEnabled;
    }

    @Override
    public boolean isConstreamMatchEnabled() {
        return constraintMatchEnabled;
    }

    @Override
    public <Score_ extends Score<Score_>, Solution_> InnerScoreDirector<Solution_, Score_> buildScoreDirector(
            SolutionDescriptor<Solution_> solutionDescriptorSupplier, ConstraintProvider constraintProvider) {
        return (InnerScoreDirector<Solution_, Score_>) new BavetConstraintStreamScoreDirectorFactory<>(
                solutionDescriptorSupplier, constraintProvider, EnvironmentMode.REPRODUCIBLE)
                .buildScoreDirector(false, constraintMatchEnabled);
    }

    @Override
    public <Solution_> ConstraintFactory buildConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptorSupplier) {
        return new BavetConstraintFactory<>(solutionDescriptorSupplier, EnvironmentMode.REPRODUCIBLE);
    }
}
