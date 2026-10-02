package ai.timefold.solver.service.testmodel;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.service.testmodel.domain.Shift;

import org.jspecify.annotations.NonNull;

public class EmployeeScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                //hard constraints
                employeeMustHaveRequiredSkill(constraintFactory),

                // medium score constraint to encourage assigning shifts to employees
                assignShiftToEmployee(constraintFactory)
        };
    }

    Constraint employeeMustHaveRequiredSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> shift.getEmployee() != null && !shift.getEmployee().isQualified(shift))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Employee must have required skill");
    }

    Constraint assignShiftToEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(Shift.class)
                .filter(shift -> shift.getEmployee() == null)
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Assign shift to employee");
    }
}
