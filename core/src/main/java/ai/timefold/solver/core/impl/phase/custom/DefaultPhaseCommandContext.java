package ai.timefold.solver.core.impl.phase.custom;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningSolutionMetaModel;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class DefaultPhaseCommandContext<Solution_> implements PhaseCommandContext<Solution_> {

    private final MoveDirector<Solution_, ?> moveDirector;
    private final BooleanSupplier isPhaseTerminated;

    public DefaultPhaseCommandContext(MoveDirector<Solution_, ?> moveDirector, BooleanSupplier isPhaseTerminated) {
        this.moveDirector = Objects.requireNonNull(moveDirector);
        this.isPhaseTerminated = Objects.requireNonNull(isPhaseTerminated);
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return moveDirector.getSolutionMetaModel();
    }

    @Override
    public Solution_ getWorkingSolution() {
        return moveDirector.getScoreDirector().getWorkingSolution();
    }

    @Override
    public boolean isPhaseTerminated() {
        return isPhaseTerminated.getAsBoolean();
    }

    @Override
    public <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity) {
        return moveDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    @Override
    public void execute(Move<Solution_> move) {
        moveDirector.execute(move, false);
    }

    @Override
    public <Score_ extends Score<Score_>> Score_ executeAndCalculateScore(Move<Solution_> move) {
        moveDirector.execute(move, true);
        return moveDirector.getScoreDirector().getSolutionDescriptor().getScore(getWorkingSolution());
    }

    @Override
    public @Nullable <Result_> Result_ executeTemporarily(Move<Solution_> move,
            Function<Solution_, Result_> temporarySolutionConsumer) {
        return moveDirector.executeTemporary(move, temporarySolutionConsumer, false);
    }

    @Override
    public <Score_ extends Score<Score_>> Score_ executeTemporarily(Move<Solution_> move) {
        return executeTemporarily(move, solution -> {
            // The score is not recalculated in executeTemporarily, so we need to do it ourselves.
            return ((DefaultPlanningSolutionMetaModel<Solution_>) getSolutionMetaModel())
                    .solutionDescriptor()
                    .getScore(solution);
        });
    }

    @Override
    public @Nullable <Result_> Result_ executeTemporarilyAndCalculateScore(Move<Solution_> move,
            Function<Solution_, Result_> temporarySolutionConsumer) {
        return moveDirector.executeTemporary(move, temporarySolutionConsumer, true);
    }

    @Override
    public <Score_ extends Score<Score_>> Score_ executeTemporarilyAndCalculateScore(Move<Solution_> move) {
        return executeTemporarilyAndCalculateScore(move, solution -> {
            // The score is not recalculated in executeTemporarily, so we need to do it ourselves.
            return ((DefaultPlanningSolutionMetaModel<Solution_>) getSolutionMetaModel())
                    .solutionDescriptor()
                    .getScore(solution);
        });
    }

}
