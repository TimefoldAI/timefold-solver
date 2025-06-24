package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SingleDirectionalParentVariableReferenceGraph<Solution_> implements VariableReferenceGraph<Solution_> {
    private final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final List<VariableUpdaterInfo<Solution_>> sortedVariableUpdaterInfoList;
    private final Set<Object> changedSet;
    private final Function<Object, Object> successorFunction;
    private final Comparator<Object> comparator;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    private final ShadowVariableLoopedVariableDescriptor<Solution_> loopedDescriptor;

    public SingleDirectionalParentVariableReferenceGraph(
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedDeclarativeShadowVariableDescriptors,
            Function<Object, Object> successorFunction, Comparator<Object> comparator,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        variableReferenceToInstanceMap = CollectionUtils.newHashMap(sortedDeclarativeShadowVariableDescriptors.size());
        sortedVariableUpdaterInfoList = new ArrayList<>(sortedDeclarativeShadowVariableDescriptors.size());
        monitoredSourceVariableSet = new HashSet<>();
        changedSet = Collections.newSetFromMap(CollectionUtils.newIdentityHashMap(entities.length));

        this.successorFunction = successorFunction;
        this.comparator = comparator;
        this.changedVariableNotifier = changedVariableNotifier;
        this.loopedDescriptor =
                sortedDeclarativeShadowVariableDescriptors.get(0).getEntityDescriptor().getShadowVariableLoopedDescriptor();

        for (var variableDescriptor : sortedDeclarativeShadowVariableDescriptors) {
            var variableMetaModel = variableDescriptor.getVariableMetaModel();
            var objectMap = CollectionUtils.newIdentityHashMap(entities.length);
            var variableUpdaterInfo = new VariableUpdaterInfo<>(
                    variableMetaModel,
                    variableDescriptor,
                    loopedDescriptor,
                    variableDescriptor.getMemberAccessor(),
                    variableDescriptor.getCalculator()::executeGetter);
            sortedVariableUpdaterInfoList.add(variableUpdaterInfo);
            variableReferenceToInstanceMap.put(variableMetaModel, CollectionUtils.newIdentityHashMap(entities.length));

            for (var source : variableDescriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }

            for (var entity : entities) {
                // No graph, so no graph id
                if (variableDescriptor.getEntityDescriptor().getEntityClass().isInstance(entity)) {
                    objectMap.put(entity, new EntityVariablePair<>(entity, variableUpdaterInfo, -1));
                    changedSet.add(entity);
                }
            }
        }
    }

    @Override
    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        return variableReferenceToInstanceMap.get(variableId).get(entity);
    }

    @Override
    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        markChanged(to);
    }

    @Override
    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        markChanged(to);
    }

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        changedSet.add(node.entity());
    }

    @Override
    public void updateChanged() {
        var changedSorted = new TreeSet<>(comparator);
        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        changedSorted.addAll(changedSet);
        for (var changed : changedSorted) {
            if (visited.contains(changed)) {
                continue;
            }

            var current = changed;
            while (current != null) {
                visited.add(current);
                var anyChanged = false;
                if (loopedDescriptor != null) {
                    var oldValue = loopedDescriptor.getValue(current);
                    if (!Objects.equals(oldValue, false)) {
                        anyChanged = true;
                        changedVariableNotifier.beforeVariableChanged().accept(loopedDescriptor, current);
                        loopedDescriptor.setValue(current, false);
                        changedVariableNotifier.afterVariableChanged().accept(loopedDescriptor, current);
                    }
                }
                for (var updater : sortedVariableUpdaterInfoList) {
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
        changedSet.clear();
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (monitoredSourceVariableSet.contains(variableReference)) {
            changedSet.add(entity);
        }
    }
}
