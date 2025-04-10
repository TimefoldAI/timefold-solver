package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultShadowVariableSession<Solution_> implements ShadowVariableSession, Supply {
    final VariableReferenceGraph<Solution_> graph;

    record EntityVariablePair<Solution_>(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof EntityVariablePair<?> other) {
                return entity == other.entity && Objects.equals(variableDescriptor, other.variableDescriptor);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(entity), variableDescriptor);
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

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        //        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        //        graph.afterVariableChanged(new VariableId(entityClass, variableDescriptor.getVariableName()),
        //                entity);
    }

    @Override
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

    // Unsupported methods
    @Override
    public void setVariable(Object entity, String variableName, @Nullable Object value) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }
}
