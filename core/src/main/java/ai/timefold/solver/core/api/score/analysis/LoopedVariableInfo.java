package ai.timefold.solver.core.api.score.analysis;

import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;

/**
 * A set of entity-variable pairs that form a cycle.
 * 
 * @param involvedVariableSet
 */
@NullMarked
public record LoopedVariableInfo(Set<EntityVariablePair> involvedVariableSet) {
    /**
     * Get the set of involved entities in the cycle
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getEntitySet() {
        return (Set<T>) involvedVariableSet.stream()
                .map(EntityVariablePair::entity)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return involvedVariableSet.stream()
                .map(EntityVariablePair::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
