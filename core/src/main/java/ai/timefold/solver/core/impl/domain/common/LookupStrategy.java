package ai.timefold.solver.core.impl.domain.common;

import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface LookupStrategy
        permits ImmutableLookupStrategy, NoneLookupStrategy, PlanningIdLookupStrategy {

    void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject);

}
