package ai.timefold.solver.core.impl.move;

import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.test.MoveTestContext;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveTester<Solution_> implements MoveTester<Solution_> {

    private final AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory;

    public DefaultMoveTester(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this(new MoveTesterScoreDirectorFactory<>(
                ((DefaultPlanningSolutionMetaModel<Solution_>) Objects.requireNonNull(solutionMetaModel)).solutionDescriptor(),
                EnvironmentMode.FULL_ASSERT));
    }

    private DefaultMoveTester(AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory) {
        this.scoreDirectorFactory = Objects.requireNonNull(scoreDirectorFactory, "scoreDirectorFactory");
    }

    @Override
    public MoveTestContext<Solution_> using(Solution_ solution) {
        // Create a score director from the cached factory
        var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(false)
                .build();
        // Set the working solution, which triggers shadow variable initialization
        scoreDirector.setWorkingSolution(Objects.requireNonNull(solution, "solution"));

        return new DefaultMoveTestContext<>(scoreDirector);
    }

}
