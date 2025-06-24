package ai.timefold.solver.core.impl.score.director;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
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
 * Call {@link #resetWorkingSolution(Object)} every time the working solution changes through a problem fact,
 * so that all caches can be invalidated.
 *
 * <p>
 * Call {@link #markEntityDependentValueRangesAsInvalid(Object)} every time an entity is changed,
 * so that the cached value range can be invalidated.
 * The actual invalidation happens at {@link #stepEnded(AbstractStepScope)}, called by the solver.
 * Otherwise every undone move would be invalidating the value ranges.
 * Value ranges should only be invalidated when a move has been selected at the end of a step.
 */
@NullMarked
public final class ValueRangeState<Solution_>
        implements PhaseLifecycleListener<Solution_> {

    private final HashSet<Object> entitiesWithInvalidValueRangesSet = new HashSet<>();
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

    void markEntityDependentValueRangesAsInvalid(Object entity) {
        entitiesWithInvalidValueRangesSet.add(entity);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // No need to do anything here.
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // No need to do anything here.
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // No need to do anything here.
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        entitiesWithInvalidValueRangesSet.forEach(fromEntityValueRangeMap::remove);
        entitiesWithInvalidValueRangesSet.clear();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // No need to do anything here.
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        this.workingSolution = null;
        fromSolutionValueRangeMap.clear();
        fromEntityValueRangeMap.clear();
    }
}
