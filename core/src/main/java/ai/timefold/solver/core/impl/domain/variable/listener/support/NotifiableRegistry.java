package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

final class NotifiableRegistry<Solution_> {

    private final List<Notifiable> notifiableList = new ArrayList<>();
    private final Set<EntityNotifiable<Solution_>>[] sourceEntityToNotifiableSetArray;
    private final List<Notifiable>[][] sourceVariableToNotifiableListArray;

    NotifiableRegistry(SolutionDescriptor<Solution_> solutionDescriptor) {
        var entityDescriptorList = solutionDescriptor.getEntityDescriptors();
        int entityDescriptorListSize = entityDescriptorList.size();
        sourceEntityToNotifiableSetArray = new Set[entityDescriptorListSize];
        sourceVariableToNotifiableListArray = new List[entityDescriptorListSize][];
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            var declaredVariableDescriptorList = entityDescriptor.getDeclaredVariableDescriptors();
            var array = new List[declaredVariableDescriptorList.size()];
            for (var variableDescriptor : declaredVariableDescriptorList) {
                array[variableDescriptor.getOrdinal()] = new ArrayList<>();
            }
            var entityDescriptorId = entityDescriptor.getOrdinal();
            sourceVariableToNotifiableListArray[entityDescriptorId] = array;
            sourceEntityToNotifiableSetArray[entityDescriptorId] = new LinkedHashSet<>();
        }
    }

    void registerNotifiable(VariableDescriptor<Solution_> source, EntityNotifiable<Solution_> notifiable) {
        registerNotifiable(Collections.singletonList(source), notifiable);
    }

    void registerNotifiable(Collection<VariableDescriptor<Solution_>> sources, EntityNotifiable<Solution_> notifiable) {
        for (VariableDescriptor<?> source : sources) {
            var entityDescriptorId = source.getEntityDescriptor().getOrdinal();
            sourceVariableToNotifiableListArray[entityDescriptorId][source.getOrdinal()].add(notifiable);
            sourceEntityToNotifiableSetArray[entityDescriptorId].add(notifiable);
        }
        notifiableList.add(notifiable);
    }

    Iterable<Notifiable> getAll() {
        return notifiableList;
    }

    Collection<EntityNotifiable<Solution_>> get(EntityDescriptor<?> entityDescriptor) {
        return sourceEntityToNotifiableSetArray[entityDescriptor.getOrdinal()];
    }

    Collection<VariableListenerNotifiable<Solution_>> get(VariableDescriptor<?> variableDescriptor) {
        var notifiables =
                sourceVariableToNotifiableListArray[variableDescriptor.getEntityDescriptor().getOrdinal()][variableDescriptor
                        .getOrdinal()];
        if (notifiables == null) {
            return Collections.emptyList();
        }
        return (Collection) notifiables;
    }

    Collection<ListVariableListenerNotifiable<Solution_>> get(ListVariableDescriptor<?> variableDescriptor) {
        var notifiables =
                sourceVariableToNotifiableListArray[variableDescriptor.getEntityDescriptor().getOrdinal()][variableDescriptor
                        .getOrdinal()];
        if (notifiables == null) {
            return Collections.emptyList();
        }
        return (Collection) notifiables;
    }
}
