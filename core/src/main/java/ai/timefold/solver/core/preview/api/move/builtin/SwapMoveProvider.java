package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.List;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SwapMoveIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * For every pair of distinct entities of the entity class,
 * creates a move that swaps the values of every variable given to the constructor,
 * provided at least one variable differs and every differing variable is legal on both entities;
 * if any differing variable is out of range, the pair is skipped entirely.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 */
@NullMarked
public final class SwapMoveProvider<Solution_, Entity_>
        implements MoveProvider<Solution_> {

    private final GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel;
    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;

    /**
     * As defined by {@link #SwapMoveProvider(List)},
     * but for every basic planning variable of {@code entityMetaModel}.
     */
    public SwapMoveProvider(GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        this(MoveProviderUtil.basicVariablesOf(entityMetaModel));
    }

    /**
     * As defined by {@link #SwapMoveProvider(List)}, but for a single variable.
     */
    public SwapMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, ?> variableMetaModel) {
        this(List.of(variableMetaModel));
    }

    /**
     * A pair is proposed only when at least one listed variable differs
     * and every differing variable is legal on both entities;
     * if any differing variable is out of range, the pair is skipped entirely.
     * All variables must belong to the same entity class.
     *
     * @param variableMetaModelList must not be empty
     */
    public SwapMoveProvider(List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList) {
        this.variableMetaModelList = MoveProviderUtil.normalize(variableMetaModelList);
        this.entityMetaModel = this.variableMetaModelList.getFirst().entity();
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var entityDataset = moveStreamFactory.forEach(entityMetaModel.type(), false).asCachedDataset();
        // We do not exclude duplicate swaps (A<>B and B<>A) to keep it simple and fast.
        return moveStreamFactory.buildMoveStream((session, random) -> new SwapMoveIterator<>(variableMetaModelList,
                session.getSolutionView(), session.getInstance(entityDataset), random));
    }

}
