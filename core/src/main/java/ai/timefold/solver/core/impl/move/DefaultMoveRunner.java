package ai.timefold.solver.core.impl.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;
import ai.timefold.solver.core.preview.api.move.MoveRunner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveRunner<Solution_> implements MoveRunner<Solution_> {

    private final AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory;
    private boolean closed = false;

    public DefaultMoveRunner(SolutionDescriptor<Solution_> solutionDescriptor) {
        this(new MoveRunnerScoreDirectorFactory<>(solutionDescriptor, null));
    }

    public DefaultMoveRunner(PlanningSolutionMetaModel<Solution_> solutionMetaModel, MoveRepository<Solution_> moveRepository) {
        this(new MoveRunnerScoreDirectorFactory<>(
                ((DefaultPlanningSolutionMetaModel<Solution_>) solutionMetaModel).solutionDescriptor(), moveRepository));
    }

    private DefaultMoveRunner(AbstractScoreDirectorFactory<Solution_, ?, ?> scoreDirectorFactory) {
        this.scoreDirectorFactory = Objects.requireNonNull(scoreDirectorFactory, "scoreDirectorFactory");
    }

    @Override
    public MoveRunContext<Solution_> using(Solution_ solution) {
        if (closed) {
            throw new IllegalStateException("""
                    The MoveRunner has been closed and cannot be reused.
                    Maybe you forgot to create a new MoveRunner instance within the try-with-resources block?
                    """);
        }

        // Create a score director from the cached factory
        var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(false)
                .build();
        // Set the working solution, which triggers shadow variable initialization
        scoreDirector.setWorkingSolution(Objects.requireNonNull(solution, "solution"));

        return new DefaultMoveRunContext<>(scoreDirector);
    }

}
