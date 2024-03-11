package ai.timefold.solver.core.impl.domain.solution.mutation;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class MutationCounter<Solution_> {

    protected final SolutionDescriptor<Solution_> solutionDescriptor;

    public MutationCounter(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    /**
     *
     * @param a never null
     * @param b never null
     * @return {@code >= 0}, the number of planning variables that have a different value in {@code a} and {@code b}.
     */
    public int countMutations(Solution_ a, Solution_ b) {
        int mutationCount = 0;
        for (EntityDescriptor<Solution_> entityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            List<Object> aEntities = entityDescriptor.extractEntities(a);
            List<Object> bEntities = entityDescriptor.extractEntities(b);
            for (Iterator<Object> aIt = aEntities.iterator(), bIt = bEntities.iterator(); aIt.hasNext() && bIt.hasNext();) {
                Object aEntity = aIt.next();
                Object bEntity = bIt.next();
                for (GenuineVariableDescriptor<Solution_> variableDescriptor : entityDescriptor
                        .getGenuineVariableDescriptorList()) {
                    if (variableDescriptor.isListVariable()) {
                        ListVariableDescriptor<Solution_> listVariableDescriptor =
                                (ListVariableDescriptor<Solution_>) variableDescriptor;
                        List<Object> aValues = listVariableDescriptor.getValue(aEntity);
                        List<Object> bValues = listVariableDescriptor.getValue(bEntity);
                        int aSize = aValues.size();
                        int bSize = bValues.size();
                        if (aSize != bSize) {
                            // First add mutations for the elements that are missing in one list.
                            mutationCount += Math.abs(aSize - bSize);
                        }
                        // Then iterate over the list and count every item that is different.
                        int shorterListSize = Math.min(aSize, bSize);
                        for (int i = 0; i < shorterListSize; i++) {
                            Object aValue = aValues.get(i);
                            Object bValue = bValues.get(i);
                            if (areDifferent(aValue, bValue)) {
                                mutationCount++;
                            }
                        }
                    } else {
                        Object aValue = variableDescriptor.getValue(aEntity);
                        Object bValue = variableDescriptor.getValue(bEntity);
                        if (areDifferent(aValue, bValue)) {
                            mutationCount++;
                        }
                    }
                }
            }
            if (aEntities.size() != bEntities.size()) {
                mutationCount += Math.abs(aEntities.size() - bEntities.size())
                        * entityDescriptor.getGenuineVariableDescriptorList().size();
            }
        }
        return mutationCount;
    }

    private boolean areDifferent(Object aValue, Object bValue) {
        EntityDescriptor<Solution_> aValueEntityDescriptor =
                solutionDescriptor.findEntityDescriptor(aValue.getClass());
        EntityDescriptor<Solution_> bValueEntityDescriptor =
                solutionDescriptor.findEntityDescriptor(bValue.getClass());
        if (aValueEntityDescriptor == null && bValueEntityDescriptor == null) { // Neither are entities.
            if (aValue == bValue) {
                return false;
            }
            return areDifferentPlanningIds(aValue, bValue);
        } else if (aValueEntityDescriptor != null && bValueEntityDescriptor != null) {
            /*
             * Both are entities.
             * Entities will all be cloned and therefore different.
             * But maybe they have the same planning ID?
             */
            if (aValueEntityDescriptor != bValueEntityDescriptor) {
                // Different entities means mutation guaranteed.
                return true;
            }
            return areDifferentPlanningIds(aValue, bValue);
        } else { // One is entity and the other one is not.
            return true;
        }
    }

    private boolean areDifferentPlanningIds(Object aValue, Object bValue) {
        MemberAccessor aIdAccessor = solutionDescriptor.getPlanningIdAccessor(aValue.getClass());
        MemberAccessor bIdAccessor = solutionDescriptor.getPlanningIdAccessor(bValue.getClass());
        if (aIdAccessor != null && bIdAccessor != null) {
            Object aId = aIdAccessor.executeGetter(aValue);
            Object bId = bIdAccessor.executeGetter(bValue);
            return !aId.equals(bId);
        } else {
            return aValue != bValue; // This counts all entities that get as far as here.
        }
    }

    @Override
    public String toString() {
        return "MutationCounter(" + solutionDescriptor + ")";
    }

}
