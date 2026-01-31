package ai.timefold.solver.core.impl.neighborhood;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.move.DefaultMoveRunContext;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodEvaluationContext;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodEvaluator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhoodEvaluator<Solution_>
        implements NeighborhoodEvaluator<Solution_> {

    private final MoveProvider<Solution_> moveProvider;
    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final MoveRunner<Solution_> moveRunner;

    public DefaultNeighborhoodEvaluator(MoveProvider<Solution_> moveProvider,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this.moveProvider = Objects.requireNonNull(moveProvider, "moveProvider");
        this.moveRunner = MoveRunner.build(solutionMetaModel);
        var solutionDescriptor = ((DefaultPlanningSolutionMetaModel<Solution_>) solutionMetaModel).solutionDescriptor();
        this.moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.FULL_ASSERT);
    }

    @Override
    public NeighborhoodEvaluationContext<Solution_> using(Solution_ solution) {
        var repository = new NeighborhoodsBasedMoveRepository<>(moveStreamFactory, List.of(moveProvider), false);
        var moveRunContext = (DefaultMoveRunContext<Solution_>) moveRunner.using(solution);
        var scoreDirector = moveRunContext.getScoreDirector();
        var solverScope = new SolverScope<Solution_>();
        solverScope.setScoreDirector(scoreDirector);
        repository.solvingStarted(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        repository.phaseStarted(phaseScope);
        return new DefaultNeighborhoodEvaluationContext<>(repository, moveRunContext, phaseScope);
    }

}
