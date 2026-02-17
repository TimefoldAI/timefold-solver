package ai.timefold.solver.core.impl.domain.lookup;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface LookUpStrategy
        permits ImmutableLookUpStrategy, NoneLookUpStrategy, PlanningIdLookUpStrategy {

    void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject);

    <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject);

    <E> @Nullable E lookUpWorkingObjectIfExists(Map<Object, Object> idToWorkingObjectMap, E externalObject);

}
