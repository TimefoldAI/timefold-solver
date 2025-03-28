package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class IntermediateShadowVariableReference<Solution_, Entity_, Value_>
        extends AbstractShadowVariableReference<Solution_, Entity_, Value_> {
    final IdentityHashMap<Entity_, Value_> intermediateValueMap;

    public IntermediateShadowVariableReference(
            SolutionDescriptor<Solution_> solutionDescriptor,
            SupplyManager supplyManager,
            String intermediateName,
            ShadowVariableCalculation<Solution_, Entity_, Value_> calculation,
            List<InnerVariableReference<Solution_, ?, ?>> shadowVariableReferences,
            Class<? extends Entity_> entityClass,
            Class<? extends Value_> valueType,
            boolean allowsNulls) {
        super(calculation.shadowVariableFactory,
                solutionDescriptor, supplyManager, null,
                new IntermediateGraphNavigator<>(VariableId.entity(entityClass),
                        entityClass,
                        valueType,
                        intermediateName,
                        calculation.shadowVariableFactory.getIntermediateValueMap(intermediateName)),
                entityClass,
                entityClass, valueType, allowsNulls,
                calculation,
                shadowVariableReferences);
        this.intermediateValueMap = calculation.shadowVariableFactory.getIntermediateValueMap(intermediateName);
    }

    @Override
    boolean update(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object object) {
        @SuppressWarnings("unchecked")
        var entity = (Entity_) object;
        var oldValue = intermediateValueMap.get(entity);
        var newValue = calculation.calculate(entity);

        var changed = false;
        if (!Objects.equals(oldValue, newValue)) {
            intermediateValueMap.put(entity, newValue);
            changed = true;
        }
        changed |= markValid(changedVariableNotifier, entity);
        return changed;
    }

    @Override
    void invalidate(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object object) {
        @SuppressWarnings("unchecked")
        var entity = (Entity_) object;
        var oldValue = intermediateValueMap.get(entity);
        if (oldValue != null) {
            intermediateValueMap.put(entity, null);
        }
        markInvalid(changedVariableNotifier, entity);
    }
}
