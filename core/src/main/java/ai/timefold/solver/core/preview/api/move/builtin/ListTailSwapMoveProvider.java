package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.move.builtin.ListTailSwapMoveIterator;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * Draws two assigned values on different entities and creates a move to swap their tails -
 * the portion of each entity's {@link PlanningListVariable list variable} from the drawn position to the end -
 * optionally in reverse element order.
 * <p>
 * This is the cross-entity half of the classic 2-opt route-improving move, at full rate.
 * {@link TwoOptListMoveProvider} makes this same move too,
 * whenever its own {@code crossingEntity} is {@code true} -
 * but there, only non-reversing, and only as one destination row among many,
 * so it arrives rarely.
 * This class exists to make it happen often, and to also emit the reversing variant.
 * <p>
 * The swapped tails are unbounded by design:
 * a tail swap is defined by the tails,
 * and bounding either one would produce a sub-list relocation instead,
 * which {@code SubListChangeMoveProvider} already provides with a size cap.
 *
 * @see TwoOptListMoveProvider The same-entity reversal, and this same shape at a much lower rate.
 * @see SubListChangeMoveProvider A bounded-size relocation instead of a whole tail.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class ListTailSwapMoveProvider<Solution_, Entity_, Value_> implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean selectReversingMoveToo;

    public ListTailSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, true);
    }

    /**
     * @param selectReversingMoveToo if {@code true}, also emits the reversing tail swap variant
     */
    public ListTailSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean selectReversingMoveToo) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new ListTailSwapMoveIterator<>(session, random,
                variableMetaModel, (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset),
                selectReversingMoveToo));
    }

    /**
     * @return {@code ListTailSwapMoveProvider(<Entity>.<variable>,reversing|noReversing)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(ListTailSwapMoveProvider.class, variableMetaModel,
                MoveProviderUtil.flag("reversing", selectReversingMoveToo));
    }

}
