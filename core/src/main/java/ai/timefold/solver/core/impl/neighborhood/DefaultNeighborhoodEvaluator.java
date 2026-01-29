package ai.timefold.solver.core.impl.neighborhood;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.move.DefaultMoveRunContext;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.neighborhood.EvaluatedNeighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodEvaluator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhoodEvaluator<Solution_>
        implements NeighborhoodEvaluator<Solution_> {

    private final Class<MoveProvider<Solution_>> moveProviderClass;
    private final PlanningSolutionMetaModel<Solution_> solutionMetaModel;
    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final MoveRunner<Solution_> moveRunner;

    public DefaultNeighborhoodEvaluator(Class<MoveProvider<Solution_>> moveProviderClass,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this.moveProviderClass = Objects.requireNonNull(moveProviderClass, "moveProviderClass");
        this.solutionMetaModel = Objects.requireNonNull(solutionMetaModel, "solutionMetaModel");
        this.moveRunner = MoveRunner.build(solutionMetaModel);
        var solutionDescriptor = ((DefaultPlanningSolutionMetaModel<Solution_>) solutionMetaModel).solutionDescriptor();
        this.moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.FULL_ASSERT);
    }

    @Override
    public EvaluatedNeighborhood<Solution_> evaluate(Solution_ solution) {
        try {
            var moveProvider = moveProviderClass.getDeclaredConstructor()
                    .newInstance();
            var repository = new NeighborhoodsBasedMoveRepository<>(moveStreamFactory, List.of(moveProvider), false);
            var moveRunContext = (DefaultMoveRunContext<Solution_>) moveRunner.using(solution);

            repository.initialize(new SessionContext<>(moveRunContext.getScoreDirector()));
            var solverScope = new SolverScope<Solution_>();
            repository.solvingStarted(solverScope);
            var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
            repository.phaseStarted(phaseScope);
            return new DefaultEvaluatedNeighborhood<>(repository, moveRunContext, phaseScope);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("""
                    The moveProviderClass (%s) cannot be instantiated.
                    Maybe introduce a public no-arg constructor?"""
                    .formatted(moveProviderClass.getCanonicalName()), e);
        }
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return solutionMetaModel;
    }

}
