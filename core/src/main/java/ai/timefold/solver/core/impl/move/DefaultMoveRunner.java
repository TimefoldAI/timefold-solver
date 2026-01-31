package ai.timefold.solver.core.impl.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;
import ai.timefold.solver.core.preview.api.move.MoveRunner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveRunner<Solution_> implements MoveRunner<Solution_> {

    private final AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory;

    public DefaultMoveRunner(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this(new MoveRunnerScoreDirectorFactory<>(
                ((DefaultPlanningSolutionMetaModel<Solution_>) Objects.requireNonNull(solutionMetaModel))
                        .solutionDescriptor()));
    }

    private DefaultMoveRunner(AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory) {
        this.scoreDirectorFactory = Objects.requireNonNull(scoreDirectorFactory, "scoreDirectorFactory");
    }

    @Override
    public MoveRunContext<Solution_> using(Solution_ solution) {
        // Create a score director from the cached factory
        var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(false)
                .build();
        // Set the working solution, which triggers shadow variable initialization
        scoreDirector.setWorkingSolution(Objects.requireNonNull(solution, "solution"));

        return new DefaultMoveRunContext<>(scoreDirector);
    }

}
