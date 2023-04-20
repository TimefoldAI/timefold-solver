package ai.timefold.solver.examples.projectjobscheduling.optional.score.common;

import ai.timefold.solver.examples.projectjobscheduling.domain.Allocation;
import ai.timefold.solver.examples.projectjobscheduling.domain.ResourceRequirement;
import ai.timefold.solver.examples.projectjobscheduling.domain.resource.Resource;

public abstract class ResourceCapacityTracker {

    protected Resource resource;

    public ResourceCapacityTracker(Resource resource) {
        this.resource = resource;
    }

    public abstract void insert(ResourceRequirement resourceRequirement, Allocation allocation);

    public abstract void retract(ResourceRequirement resourceRequirement, Allocation allocation);

    public abstract int getHardScore();

}
