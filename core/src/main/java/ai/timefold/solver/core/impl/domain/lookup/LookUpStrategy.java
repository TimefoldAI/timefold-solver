package ai.timefold.solver.core.impl.domain.lookup;

import java.util.Map;

public sealed interface LookUpStrategy
        permits EqualsLookUpStrategy, ImmutableLookUpStrategy, NoneLookUpStrategy, PlanningIdLookUpStrategy {

    void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject);

    <E> E lookUpWorkingObjectIfExists(Map<Object, Object> idToWorkingObjectMap, E externalObject);

}
