package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.HashSet;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultShadowVariableSession<Solution_> implements Supply {
    final VariableReferenceGraph<Solution_> graph;

    record EntityVariablePair(VariableMetaModel<?, ?, ?> variableMetamodel, Object entity) {
        // entity must be compared by identity; cannot rely on user's equals/hashCode.
        // variableMetamodel is guaranteed to be the same instance for the same variable.
        // this class is often used as a key for maps, so equals/hashCode are optimized for performance.
        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof EntityVariablePair other) {
                return entity == other.entity && variableMetamodel == other.variableMetamodel;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (31 * System.identityHashCode(entity)) ^ System.identityHashCode(variableMetamodel);
        }
    }

    final Set<EntityVariablePair> modifiedEntityVariableSet = new HashSet<>();

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (modifiedEntityVariableSet.add(new EntityVariablePair(variableDescriptor.getVariableMetaModel(), entity))) {
            graph.beforeVariableChanged(variableDescriptor.getVariableMetaModel(),
                    entity);
        }
    }

    public void updateVariables() {
        for (var modifiedEntityVariable : modifiedEntityVariableSet) {
            graph.afterVariableChanged(
                    modifiedEntityVariable.variableMetamodel,
                    modifiedEntityVariable.entity);
        }
        modifiedEntityVariableSet.clear();
        graph.updateChanged();
    }
}
