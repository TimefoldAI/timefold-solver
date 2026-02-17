package ai.timefold.solver.core.impl.phase.custom;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;
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
    public <T> T lookupWorkingObject(T original) {
        return moveDirector.rebase(original);
    }

    @Override
    public void execute(Move<Solution_> move, boolean guaranteeFreshScore) {
        moveDirector.execute(move, guaranteeFreshScore);
    }

    @Override
    public @Nullable <Result_> Result_ executeTemporarily(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> temporarySolutionConsumer, boolean guaranteeFreshScore) {
        return moveDirector.executeTemporary(move, temporarySolutionConsumer, guaranteeFreshScore);
    }

}
