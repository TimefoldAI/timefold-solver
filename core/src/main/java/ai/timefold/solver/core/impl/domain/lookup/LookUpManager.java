package ai.timefold.solver.core.impl.domain.lookup;

import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @see PlanningId
 * @see ScoreDirector#lookUpWorkingObject(Object)
 */
@NullMarked
public final class LookUpManager {

    private final LookUpStrategyResolver lookUpStrategyResolver;
    private final Map<Object, Object> idToWorkingObjectMap = new HashMap<>();

    public LookUpManager(LookUpStrategyResolver lookUpStrategyResolver) {
        this.lookUpStrategyResolver = lookUpStrategyResolver;
    }

    public void reset() {
        idToWorkingObjectMap.clear();
    }

    public void addWorkingObject(Object workingObject) {
        var lookUpStrategy = lookUpStrategyResolver.determineLookUpStrategy(workingObject);
        lookUpStrategy.addWorkingObject(idToWorkingObjectMap, workingObject);
    }

    public void removeWorkingObject(Object workingObject) {
        var lookUpStrategy = lookUpStrategyResolver.determineLookUpStrategy(workingObject);
        lookUpStrategy.removeWorkingObject(idToWorkingObjectMap, workingObject);
    }

    /**
     * As defined by {@link ScoreDirector#lookUpWorkingObject(Object)}.
     *
     * @return null if externalObject is null
     * @throws IllegalArgumentException if there is no workingObject for externalObject, if it cannot be looked up
     *         or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    public <E> @Nullable E lookUpWorkingObject(@Nullable E externalObject) {
        if (externalObject == null) {
            return null;
        }
        var lookUpStrategy = lookUpStrategyResolver.determineLookUpStrategy(externalObject);
        return lookUpStrategy.lookUpWorkingObject(idToWorkingObjectMap, externalObject);
    }

    /**
     * As defined by {@link ScoreDirector#lookUpWorkingObjectOrReturnNull(Object)}.
     *
     * @return null if externalObject is null, or if there is no workingObject for externalObject
     * @throws IllegalArgumentException if it cannot be looked up or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    public <E> @Nullable E lookUpWorkingObjectOrReturnNull(@Nullable E externalObject) {
        if (externalObject == null) {
            return null;
        }
        var lookUpStrategy = lookUpStrategyResolver.determineLookUpStrategy(externalObject);
        return lookUpStrategy.lookUpWorkingObjectIfExists(idToWorkingObjectMap, externalObject);
    }

}
