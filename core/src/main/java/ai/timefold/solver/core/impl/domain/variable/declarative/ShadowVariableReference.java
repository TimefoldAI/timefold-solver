package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ShadowVariableReference<Solution_, Entity_, Value_>
        extends AbstractShadowVariableReference<Solution_, Entity_, Value_> {
    final VariableDescriptor<Solution_> variableDescriptor;

    public ShadowVariableReference(
            SolutionDescriptor<Solution_> solutionDescriptor,
            SupplyManager supplyManager,
            VariableDescriptor<Solution_> variableDescriptor,
            ShadowVariableCalculation<Solution_, Entity_, Value_> calculation,
            List<InnerVariableReference<Solution_, ?, ?>> shadowVariableReferences,
            Class<? extends Entity_> entityClass,
            Class<? extends Value_> valueType,
            boolean allowsNulls) {
        super(calculation.shadowVariableFactory,
                solutionDescriptor, supplyManager, null,
                new VariableGraphNavigator<>(VariableId.entity(entityClass),
                        variableDescriptor),
                entityClass,
                entityClass, valueType, allowsNulls,
                calculation, shadowVariableReferences);
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    boolean update(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object object) {
        @SuppressWarnings("unchecked")
        var entity = (Entity_) object;
        var oldValue = variableDescriptor.getValue(entity);

        var newValue = calculation.calculate(entity);

        var changed = false;
        if (!Objects.equals(oldValue, newValue)) {
            changedVariableNotifier.beforeVariableChanged(variableDescriptor, entity);
            variableDescriptor.setValue(entity, newValue);
            changedVariableNotifier.afterVariableChanged(variableDescriptor, entity);
            changed = true;
        }
        changed |= markValid(changedVariableNotifier, entity);
        return changed;
    }

    @Override
    void invalidate(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity) {
        var oldValue = variableDescriptor.getValue(entity);
        if (oldValue != null) {
            changedVariableNotifier.beforeVariableChanged(variableDescriptor, entity);
            variableDescriptor.setValue(entity, null);
            changedVariableNotifier.afterVariableChanged(variableDescriptor, entity);
        }
        markInvalid(changedVariableNotifier, entity);
    }
}
