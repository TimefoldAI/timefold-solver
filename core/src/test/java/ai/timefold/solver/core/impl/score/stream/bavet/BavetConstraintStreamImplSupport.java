package ai.timefold.solver.core.impl.score.stream.bavet;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;

public record BavetConstraintStreamImplSupport(ConstraintMatchPolicy constraintMatchPolicy)
        implements
            ConstraintStreamImplSupport {

    @Override
    public <Score_ extends Score<Score_>, Solution_> InnerScoreDirector<Solution_, Score_> buildScoreDirector(
            SolutionDescriptor<Solution_> solutionDescriptorSupplier, ConstraintProvider constraintProvider) {
        var scoreDirectorFactory = new BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>(solutionDescriptorSupplier,
                constraintProvider, EnvironmentMode.PHASE_ASSERT);
        return scoreDirectorFactory.createScoreDirectorBuilder()
                .withConstraintMatchPolicy(constraintMatchPolicy)
                .build();
    }

    @Override
    public <Solution_> ConstraintFactory buildConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptorSupplier) {
        return new BavetConstraintFactory<>(solutionDescriptorSupplier, EnvironmentMode.PHASE_ASSERT);
    }
}
