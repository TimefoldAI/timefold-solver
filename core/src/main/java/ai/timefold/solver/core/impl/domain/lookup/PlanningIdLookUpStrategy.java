package ai.timefold.solver.core.impl.domain.lookup;

import java.util.Map;

import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.util.Pair;

public final class PlanningIdLookUpStrategy implements LookUpStrategy {

    private final MemberAccessor planningIdMemberAccessor;

    public PlanningIdLookUpStrategy(MemberAccessor planningIdMemberAccessor) {
        this.planningIdMemberAccessor = planningIdMemberAccessor;
    }

    @Override
    public void addWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        var planningId = extractPlanningId(workingObject);
        var oldAddedObject = idToWorkingObjectMap.put(planningId, workingObject);
        if (oldAddedObject != null) {
            throw new IllegalStateException("The workingObjects (" + oldAddedObject + ", " + workingObject
                    + ") have the same planningId (" + planningId + "). Working objects must be unique.");
        }
    }

    @Override
    public void removeWorkingObject(Map<Object, Object> idToWorkingObjectMap, Object workingObject) {
        var planningId = extractPlanningId(workingObject);
        var removedObject = idToWorkingObjectMap.remove(planningId);
        if (workingObject != removedObject) {
            throw new IllegalStateException("The workingObject (" + workingObject
                    + ") differs from the removedObject (" + removedObject + ") for planningId (" + planningId + ").");
        }
    }

    @Override
    public <E> E lookUpWorkingObject(Map<Object, Object> idToWorkingObjectMap, E externalObject) {
        var planningId = extractPlanningId(externalObject);
        var workingObject = (E) idToWorkingObjectMap.get(planningId);
        if (workingObject == null) {
            throw new IllegalStateException("The externalObject (" + externalObject + ") with planningId (" + planningId
                    + ") has no known workingObject (" + workingObject + ").\n"
                    + "Maybe the workingObject was never added because the planning solution doesn't have a @"
                    + ProblemFactCollectionProperty.class.getSimpleName()
                    + " annotation on a member with instances of the externalObject's class ("
                    + externalObject.getClass() + ").");
        }
        return workingObject;
    }

    @Override
    public <E> E lookUpWorkingObjectIfExists(Map<Object, Object> idToWorkingObjectMap, E externalObject) {
        var planningId = extractPlanningId(externalObject);
        return (E) idToWorkingObjectMap.get(planningId);
    }

    private Pair<Class<?>, Object> extractPlanningId(Object externalObject) {
        var planningId = planningIdMemberAccessor.executeGetter(externalObject);
        if (planningId == null) {
            throw new IllegalArgumentException("The planningId (" + planningId
                    + ") of the member (" + planningIdMemberAccessor + ") of the class (" + externalObject.getClass()
                    + ") on externalObject (" + externalObject
                    + ") must not be null.\n"
                    + "Maybe initialize the planningId of the class (" + externalObject.getClass().getSimpleName()
                    + ") instance (" + externalObject + ") before solving.\n" +
                    "Maybe remove the @" + PlanningId.class.getSimpleName() + " annotation"
                    + " or change the @" + PlanningSolution.class.getSimpleName() + " annotation's "
                    + LookUpStrategyType.class.getSimpleName() + ".");
        }
        return new Pair<>(externalObject.getClass(), planningId);
    }

}
