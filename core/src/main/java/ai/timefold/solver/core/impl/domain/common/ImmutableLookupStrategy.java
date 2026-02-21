package ai.timefold.solver.core.impl.domain.common;

import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ImmutableLookupStrategy implements LookupStrategy {

    @Override
    public void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        // Do nothing
    }

    @Override
    public void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        // Do nothing
    }

    @Override
    public <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject) {
        // Because it is immutable, we can use the same one.
        return externalObject;
    }

}
