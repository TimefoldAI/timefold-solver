package ai.timefold.solver.service.testmodel;

import ai.timefold.solver.service.definition.api.description.ConstraintReference;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class EmployeeScheduleModelConfigOverrides implements ModelConfigOverrides {

    private static final long DEFAULT_WEIGHT_ONE = 1L;

    @ConstraintReference(EmployeeScheduleConstraintProvider.ASSIGN_SHIFT_TO_EMPLOYEE)
    @Schema(minimum = "0")
    private long assignShiftToEmployeeWeight = DEFAULT_WEIGHT_ONE;

    public long getAssignShiftToEmployeeWeight() {
        return assignShiftToEmployeeWeight;
    }

    public void setAssignShiftToEmployeeWeight(long assignShiftToEmployeeWeight) {
        this.assignShiftToEmployeeWeight = assignShiftToEmployeeWeight;
    }
}
