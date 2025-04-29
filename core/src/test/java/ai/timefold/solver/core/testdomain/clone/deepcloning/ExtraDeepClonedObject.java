package ai.timefold.solver.core.testdomain.clone.deepcloning;

import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;

@DeepPlanningClone
public class ExtraDeepClonedObject {
    public String id;

    public ExtraDeepClonedObject() {
    }

    public ExtraDeepClonedObject(String id) {
        this.id = id;
    }
}
