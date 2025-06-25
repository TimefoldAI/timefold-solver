package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SingleDirectionalParentVariableReferenceGraph<Solution_> implements VariableReferenceGraph<Solution_> {
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final VariableUpdaterInfo<Solution_>[] sortedVariableUpdaterInfos;
    private final Function<Object, Object> successorFunction;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    public SingleDirectionalParentVariableReferenceGraph(
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedDeclarativeShadowVariableDescriptors,
            Function<Object, Object> successorFunction,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        var entityClass = sortedDeclarativeShadowVariableDescriptors.get(0).getEntityDescriptor().getEntityClass();
        // noinspection unchecked
        sortedVariableUpdaterInfos = new VariableUpdaterInfo[sortedDeclarativeShadowVariableDescriptors.size()];
        monitoredSourceVariableSet = new HashSet<>();

        this.successorFunction = successorFunction;
        this.changedVariableNotifier = changedVariableNotifier;
        var shadowEntities = Arrays.stream(entities).filter(entityClass::isInstance).toArray();
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
    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        throw new IllegalStateException("Impossible state: cannot lookup in a %s graph."
                .formatted(SingleDirectionalParentVariableReferenceGraph.class.getSimpleName()));
    }

    @Override
    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        throw new IllegalStateException("Impossible state: cannot modify an %s graph."
                .formatted(SingleDirectionalParentVariableReferenceGraph.class.getSimpleName()));
    }

    @Override
    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        throw new IllegalStateException("Impossible state: cannot modify an %s graph."
                .formatted(SingleDirectionalParentVariableReferenceGraph.class.getSimpleName()));
    }

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        throw new IllegalStateException("Impossible state: cannot mark changed an %s graph."
                .formatted(SingleDirectionalParentVariableReferenceGraph.class.getSimpleName()));
    }

    @Override
    public void updateChanged() {
        // Do nothing; afterVariableChanged do the update
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
        if (monitoredSourceVariableSet.contains(variableReference)) {
            updateChanged(entity);
        }
    }

    @Override
    public boolean shouldQueueAfterEvents() {
        return true;
    }
}
