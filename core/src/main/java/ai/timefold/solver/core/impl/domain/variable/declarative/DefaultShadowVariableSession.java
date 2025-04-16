package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.HashSet;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultShadowVariableSession<Solution_> implements Supply {
    final VariableReferenceGraph<Solution_> graph;

    record EntityVariablePair<Solution_>(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof EntityVariablePair<?> other) {
                return entity == other.entity && variableDescriptor.getOrdinal() == other.variableDescriptor.getOrdinal();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (31 * System.identityHashCode(entity)) ^ variableDescriptor.getOrdinal();
        }
    }

    final Set<EntityVariablePair<Solution_>> modifiedEntityVariableSet = new HashSet<>();

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (modifiedEntityVariableSet.add(new EntityVariablePair<>(variableDescriptor, entity))) {
            var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
            graph.beforeVariableChanged(new VariableId(entityClass, variableDescriptor.getVariableName()),
                    entity);
        }
    }

    public void updateVariables() {
        for (var modifiedEntityVariable : modifiedEntityVariableSet) {
            graph.afterVariableChanged(
                    new VariableId(modifiedEntityVariable.variableDescriptor.getEntityDescriptor().getEntityClass(),
                            modifiedEntityVariable.variableDescriptor.getVariableName()),
                    modifiedEntityVariable.entity);
        }
        modifiedEntityVariableSet.clear();
        graph.updateChanged();
    }
}
