package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

public final class SingleDirectionalParentVariableReferenceGraph<Solution_> implements VariableReferenceGraph {
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final VariableUpdaterInfo<Solution_>[] sortedVariableUpdaterInfos;
    private final UnaryOperator<Object> successorFunction;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    private final List<Object> changedEntities;
    private final Class<?> monitoredEntityClass;
    private boolean isUpdating;

    @SuppressWarnings("unchecked")
    public SingleDirectionalParentVariableReferenceGraph(
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedDeclarativeShadowVariableDescriptors,
            UnaryOperator<Object> successorFunction,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        monitoredEntityClass = sortedDeclarativeShadowVariableDescriptors.get(0).getEntityDescriptor().getEntityClass();
        sortedVariableUpdaterInfos = new VariableUpdaterInfo[sortedDeclarativeShadowVariableDescriptors.size()];
        monitoredSourceVariableSet = new HashSet<>();
        changedEntities = new ArrayList<>();
        isUpdating = false;

        this.successorFunction = successorFunction;
        this.changedVariableNotifier = changedVariableNotifier;
        var shadowEntities = Arrays.stream(entities).filter(monitoredEntityClass::isInstance).toArray();
        var loopedDescriptor =
                sortedDeclarativeShadowVariableDescriptors.get(0).getEntityDescriptor().getShadowVariableLoopedDescriptor();

        var updaterIndex = 0;
        for (var variableDescriptor : sortedDeclarativeShadowVariableDescriptors) {
            var variableMetaModel = variableDescriptor.getVariableMetaModel();
            var variableUpdaterInfo = new VariableUpdaterInfo<>(
                    variableMetaModel,
                    variableDescriptor,
                    loopedDescriptor,
                    variableDescriptor.getMemberAccessor(),
                    variableDescriptor.getCalculator()::executeGetter);
            sortedVariableUpdaterInfos[updaterIndex++] = variableUpdaterInfo;

            for (var source : variableDescriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }
        }

        for (var shadowEntity : shadowEntities) {
            updateChanged(shadowEntity);
        }
        if (loopedDescriptor != null) {
            for (var shadowEntity : shadowEntities) {
                changedVariableNotifier.beforeVariableChanged().accept(loopedDescriptor, shadowEntity);
                loopedDescriptor.setValue(shadowEntity, false);
                changedVariableNotifier.afterVariableChanged().accept(loopedDescriptor, shadowEntity);
            }
        }
    }

    @Override
    public void updateChanged() {
        isUpdating = true;
        for (var changedEntity : changedEntities) {
            updateChanged(changedEntity);
        }
        isUpdating = false;
        changedEntities.clear();
    }

    private void updateChanged(Object entity) {
        var current = entity;
        while (current != null) {
            var anyChanged = false;
            for (var updater : sortedVariableUpdaterInfos) {
                var oldValue = updater.memberAccessor().executeGetter(current);
                var newValue = updater.calculator().apply(current);
                if (!Objects.equals(oldValue, newValue)) {
                    anyChanged = true;
                    changedVariableNotifier.beforeVariableChanged().accept(updater.variableDescriptor(), current);
                    updater.memberAccessor().executeSetter(current, newValue);
                    changedVariableNotifier.afterVariableChanged().accept(updater.variableDescriptor(), current);
                }
            }
            if (anyChanged) {
                current = successorFunction.apply(current);
            } else {
                current = null;
            }
        }
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (!isUpdating && monitoredSourceVariableSet.contains(variableReference) && monitoredEntityClass.isInstance(entity)) {
            changedEntities.add(entity);
        }
    }

}
