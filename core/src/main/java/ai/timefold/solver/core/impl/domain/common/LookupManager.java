package ai.timefold.solver.core.impl.domain.common;

import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.common.PlanningId;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @see PlanningId
 * @see Lookup
 */
@NullMarked
public final class LookupManager
        implements Lookup {

    private final LookupStrategyResolver lookupStrategyResolver;
    private final Map<Object, Object> idToWorkingObjectMap = new HashMap<>();

    public LookupManager(LookupStrategyResolver lookupStrategyResolver) {
        this.lookupStrategyResolver = lookupStrategyResolver;
    }

    public void reset() {
        idToWorkingObjectMap.clear();
    }

    public void addWorkingObject(Object workingObject) {
        var lookupStrategy = lookupStrategyResolver.determineLookUpStrategy(workingObject);
        lookupStrategy.addWorkingObject(idToWorkingObjectMap, workingObject);
    }

    public void removeWorkingObject(Object workingObject) {
        var lookupStrategy = lookupStrategyResolver.determineLookUpStrategy(workingObject);
        lookupStrategy.removeWorkingObject(idToWorkingObjectMap, workingObject);
    }

    @Override
    public <E> @Nullable E lookUpWorkingObject(@Nullable E externalObject) {
        if (externalObject == null) {
            return null;
        }
        var lookupStrategy = lookupStrategyResolver.determineLookUpStrategy(externalObject);
        return lookupStrategy.lookUpWorkingObject(idToWorkingObjectMap, externalObject);
    }

}
