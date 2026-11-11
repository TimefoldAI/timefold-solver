package ai.timefold.solver.core.impl.move.builtin;

import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Changes a drawn entity to a value drawn from that entity's own value range,
 * so that no out-of-range value is ever drawn.
 * The current value is never chosen; null is chosen only if allowed.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the value type
 */
@NullMarked
public final class ValueRangeChangeMoveIterator<Solution_, Entity_, Value_>
        extends AbstractLeftDrivenMoveIterator<Solution_, Entity_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final SolutionView<Solution_> solutionView;
    private final RandomGenerator random;
    private final boolean nullAllowed;

    /**
     * @param entityIterator draws the entities to change, at random from the entities not yet retired
     * @param nullAllowed whether null is an acceptable new value
     */
    public ValueRangeChangeMoveIterator(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            SolutionView<Solution_> solutionView, RetiringRandomIterator<@Nullable Entity_> entityIterator,
            RandomGenerator random, boolean nullAllowed) {
        super(entityIterator);
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.solutionView = Objects.requireNonNull(solutionView);
        this.random = Objects.requireNonNull(random);
        this.nullAllowed = nullAllowed;
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Entity_ entity) {
        var currentValue = solutionView.getValue(variableMetaModel, entity);
        var valueRange = solutionView.getValueRange(variableMetaModel, entity);
        // The range may hold null as one extra element; hence the +1.
        var size = valueRange.getSize();
        var budget = bailOutBudget(size == Long.MAX_VALUE ? size : size + 1);
        var valueIterator = valueRange.createRandomIterator(random);
        for (var attempt = 0L; attempt < budget && valueIterator.hasNext(); attempt++) {
            var value = valueIterator.next();
            if (value == null ? nullAllowed && currentValue != null : !value.equals(currentValue)) {
                return Moves.change(variableMetaModel, entity, value);
            }
        }
        return null;
    }

}
