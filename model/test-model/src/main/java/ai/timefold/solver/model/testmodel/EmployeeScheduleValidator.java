package ai.timefold.solver.model.testmodel;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.definition.api.domain.ModelConfig;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;
import ai.timefold.solver.model.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.model.quarkus.deployment.defaults.EmptyModelConfigOverrides;
import ai.timefold.solver.model.testmodel.domain.EmployeeSchedule;
import ai.timefold.solver.model.testmodel.domain.Shift;

@ApplicationScoped
public class EmployeeScheduleValidator
        implements ModelValidator<EmployeeSchedule, EmptyModelConfigOverrides> {

    @Override
    public void validate(ValidationBuilder validationBuilder, EmployeeSchedule modelInput,
            ModelConfig<EmptyModelConfigOverrides> modelConfig) {
        for (var shift : modelInput.getShifts()) {
            validateShift(validationBuilder, shift);
        }
    }

    private void validateShift(ValidationBuilder builder, Shift shift) {
        if (shift.getEndTime().isBefore(shift.getStartTime())) {
            builder.addIssue(new ShiftEndBeforeStartIssue(shift.getId(), shift.getStartTime(), shift.getEndTime()));
        }
    }
}
