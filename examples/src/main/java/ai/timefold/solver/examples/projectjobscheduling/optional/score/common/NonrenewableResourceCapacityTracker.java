package ai.timefold.solver.examples.projectjobscheduling.optional.score.common;

import ai.timefold.solver.examples.projectjobscheduling.domain.Allocation;
import ai.timefold.solver.examples.projectjobscheduling.domain.ResourceRequirement;
import ai.timefold.solver.examples.projectjobscheduling.domain.resource.Resource;

public class NonrenewableResourceCapacityTracker extends ResourceCapacityTracker {

    protected int capacity;
    protected int used;

    public NonrenewableResourceCapacityTracker(Resource resource) {
        super(resource);
        if (resource.isRenewable()) {
            throw new IllegalArgumentException("The resource (" + resource + ") is expected to be nonrenewable.");
        }
        capacity = resource.getCapacity();
        used = 0;
    }

    @Override
    public void insert(ResourceRequirement resourceRequirement, Allocation allocation) {
        used += resourceRequirement.getRequirement();
    }

    @Override
    public void retract(ResourceRequirement resourceRequirement, Allocation allocation) {
        used -= resourceRequirement.getRequirement();
    }

    @Override
    public int getHardScore() {
        if (capacity >= used) {
            return 0;
        }
        return capacity - used;
    }

}
