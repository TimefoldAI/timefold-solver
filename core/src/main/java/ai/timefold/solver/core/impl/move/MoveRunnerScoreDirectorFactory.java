package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class MoveRunnerScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_, MoveRunnerScoreDirectorFactory<Solution_, Score_>> {

    @Nullable
    private final MoveRepository<Solution_> moveRepository;

    public MoveRunnerScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            @Nullable MoveRepository<Solution_> moveRepository) {
        super(solutionDescriptor);
        this.moveRepository = moveRepository;
    }

    @Override
    public AbstractScoreDirector.AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
        return new MoveRunnerScoreDirector.Builder<>(this)
                .withMoveRepository(moveRepository);
    }

}
