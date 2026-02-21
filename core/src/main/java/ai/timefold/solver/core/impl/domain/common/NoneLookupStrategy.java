package ai.timefold.solver.core.impl.domain.common;

import java.util.Map;

import ai.timefold.solver.core.api.domain.common.PlanningId;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class NoneLookupStrategy implements LookupStrategy {

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
        throw new IllegalArgumentException("""
                The externalObject (%s) cannot be looked up.
                Some functionality, such as multithreaded solving, requires this ability.
                Maybe add a @%s annotation on an identifier property of the class (%s)."""
                .formatted(externalObject, PlanningId.class.getSimpleName(), externalObject.getClass()));
    }

}
