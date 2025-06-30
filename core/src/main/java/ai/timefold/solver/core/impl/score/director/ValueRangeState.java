package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Caches value ranges for the current working solution,
 * allowing to quickly check if a value is in range.
 * Used by {@link AbstractScoreDirector#isValueInRange(GenuineVariableMetaModel, Object, Object)}.
 *
 * <p>
 * The state is built on-demand as {@link #isInRange(ValueRangeDescriptor, Object, Object)} is called.
 * It is not built in advance as it would be too expensive, and some code paths do not use it at all.
 * 
 * <p>
 * Outside a {@link ProblemChange}, value ranges are not allowed to change.
 * Call {@link #resetWorkingSolution(Object)} every time the working solution changes through a problem fact,
 * so that all caches can be invalidated.
 */
@NullMarked
public final class ValueRangeState<Solution_> {

    private final Map<ValueRangeDescriptor<Solution_>, ValueRange<Object>> fromSolutionValueRangeMap = new IdentityHashMap<>();
    private final Map<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<Object>>> fromEntityValueRangeMap =
            new IdentityHashMap<>();

    private @Nullable Solution_ workingSolution;

    void resetWorkingSolution(Solution_ workingSolution) {
        this.workingSolution = workingSolution;
        fromSolutionValueRangeMap.clear();
        fromEntityValueRangeMap.clear();
    }

    public boolean isInRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @Nullable Object entity,
            @Nullable Object value) {
        if (value == null) {
            var variableDescriptor = valueRangeDescriptor.getVariableDescriptor();
            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor) {
                return basicVariableDescriptor.allowsUnassigned();
            } else if (variableDescriptor instanceof ListVariableDescriptor<Solution_> listVariableDescriptor) {
                return listVariableDescriptor.allowsUnassignedValues();
            } else {
                throw new IllegalStateException("Impossible state: The variable descriptor (%s) is neither %s nor %s."
                        .formatted(variableDescriptor, BasicVariableDescriptor.class.getSimpleName(),
                                ListVariableDescriptor.class.getSimpleName()));
            }
        }
        var solution = Objects.requireNonNull(workingSolution);
        // Not using computeIfAbsent() here, as it would have allocated lambda instances on the hot path.
        if (valueRangeDescriptor.isEntityIndependent()) {
            var valueRange = fromSolutionValueRangeMap.get(valueRangeDescriptor);
            if (valueRange == null) {
                valueRange = valueRangeDescriptor.extractValueRange(solution, null);
                fromSolutionValueRangeMap.put(valueRangeDescriptor, valueRange);
            }
            return valueRange.contains(value);
        } else {
            var valueRangeMap = fromEntityValueRangeMap.get(entity);
            if (valueRangeMap == null) {
                valueRangeMap = new IdentityHashMap<>();
                fromEntityValueRangeMap.put(Objects.requireNonNull(entity), valueRangeMap);
            }
            var valueRange = valueRangeMap.get(valueRangeDescriptor);
            if (valueRange == null) {
                valueRange = valueRangeDescriptor.extractValueRange(solution, entity);
                valueRangeMap.put(valueRangeDescriptor, valueRange);
            }
            return valueRange.contains(value);
        }
    }

}
