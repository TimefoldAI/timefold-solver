package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DefaultShadowVariableSession<Solution_> implements Supply {
    final VariableReferenceGraph<Solution_> graph;
    final boolean shouldQueueAfterEvents;

    record VariableChangedEvent<Solution_>(VariableMetaModel<Solution_, ?, ?> variableMetaModel, Object entity) {
    }

    @Nullable
    final List<VariableChangedEvent<Solution_>> variableChangedList;

    boolean isUpdating;

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
        this.isUpdating = false;
        if (graph.shouldQueueAfterEvents()) {
            this.shouldQueueAfterEvents = true;
            this.variableChangedList = new ArrayList<>();
        } else {
            this.shouldQueueAfterEvents = false;
            this.variableChangedList = null;
        }
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        beforeVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        afterVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void beforeVariableChanged(VariableMetaModel<Solution_, ?, ?> variableMetaModel, Object entity) {
        graph.beforeVariableChanged(variableMetaModel,
                entity);
    }

    public void afterVariableChanged(VariableMetaModel<Solution_, ?, ?> variableMetaModel, Object entity) {
        if (shouldQueueAfterEvents && !isUpdating) {
            variableChangedList.add(new VariableChangedEvent<>(variableMetaModel, entity));
        } else {
            graph.afterVariableChanged(variableMetaModel,
                    entity);
        }
    }

    public void updateVariables() {
        if (shouldQueueAfterEvents) {
            isUpdating = true;
            for (var variableChanged : variableChangedList) {
                graph.afterVariableChanged(variableChanged.variableMetaModel, variableChanged.entity);
            }
            variableChangedList.clear();
            isUpdating = false;
        }
        graph.updateChanged();
    }
}
