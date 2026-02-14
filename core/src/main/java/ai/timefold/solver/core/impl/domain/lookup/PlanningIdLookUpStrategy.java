package ai.timefold.solver.core.impl.domain.lookup;

import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class PlanningIdLookUpStrategy implements LookUpStrategy {

    private final MemberAccessor planningIdMemberAccessor;

    public PlanningIdLookUpStrategy(MemberAccessor planningIdMemberAccessor) {
        this.planningIdMemberAccessor = planningIdMemberAccessor;
    }

    @Override
    public void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        var planningId = extractPlanningId(workingObject);
        var oldAddedObject = idToWorkingObjectMap.put(planningId, workingObject);
        if (oldAddedObject != null) {
            throw new IllegalStateException("""
                    The workingObjects (%s, %s) have the same planningId (%s).
                    Working objects must be unique."""
                    .formatted(oldAddedObject, workingObject, planningId));
        }
    }

    @Override
    public void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        var planningId = extractPlanningId(workingObject);
        var removedObject = idToWorkingObjectMap.remove(planningId);
        if (workingObject != removedObject) {
            throw new IllegalStateException("The workingObject (%s) differs from the removedObject (%s) for planningId (%s)."
                    .formatted(workingObject, removedObject, planningId));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject) {
        var planningId = extractPlanningId(externalObject);
        var workingObject = idToWorkingObjectMap.get(planningId);
        if (workingObject == null) {
            throw new IllegalStateException("""
                    The externalObject (%s) with planningId (%s) has no known workingObject (%s).
                    Maybe the workingObject was never added because the planning solution doesn't have a @%s annotation \
                    on a member with instances of the externalObject's class (%s)."""
                    .formatted(externalObject, planningId, workingObject, ProblemFactCollectionProperty.class.getSimpleName(),
                            externalObject.getClass()));
        }
        return (E) workingObject;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> @Nullable E lookUpWorkingObjectIfExists(Map<Object, Object> idToWorkingObjectMap, E externalObject) {
        var planningId = extractPlanningId(externalObject);
        return (E) idToWorkingObjectMap.get(planningId);
    }

    private Pair<Class<?>, Object> extractPlanningId(Object externalObject) {
        var planningId = planningIdMemberAccessor.executeGetter(externalObject);
        if (planningId == null) {
            throw new IllegalArgumentException("""
                    The planningId (%s) of the member (%s) of the class (%s) on externalObject (%s) must not be null.
                    Maybe initialize the planningId of the class (%s) instance (%s) before solving."""
                    .formatted(planningId, planningIdMemberAccessor, externalObject.getClass(), externalObject,
                            externalObject.getClass().getSimpleName(), externalObject));
        }
        return new Pair<>(externalObject.getClass(), planningId);
    }

}
