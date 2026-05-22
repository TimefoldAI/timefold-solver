package ai.timefold.solver.model.testmodel;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.model.testmodel.domain.Employee;
import ai.timefold.solver.model.testmodel.domain.EmployeeSchedule;
import ai.timefold.solver.model.testmodel.domain.Shift;
import ai.timefold.solver.model.testmodel.domain.Skill;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EmployeeScheduleProviderTest {

    @Inject
    ConstraintVerifier<EmployeeScheduleConstraintProvider, EmployeeSchedule> constraintVerifier;

    @Test
    void assignShiftToEmployeeAssignedShift() {
        Employee employeeAnn = new Employee("Ann", Skill.AMBULANCE);
        Shift ambulanceShift = new Shift("Ambulance shift 1", Skill.AMBULANCE);

        ambulanceShift.setEmployee(employeeAnn);

        constraintVerifier.verifyThat(EmployeeScheduleConstraintProvider::assignShiftToEmployee)
                .given(employeeAnn, ambulanceShift)
                .penalizesBy(0);
    }

    @Test
    void assignShiftToEmployeeUnassignedShift() {
        Employee employeeAnn = new Employee("Ann", Skill.AMBULANCE);
        Shift ambulanceShift = new Shift("Ambulance shift 1", Skill.AMBULANCE);

        constraintVerifier.verifyThat(EmployeeScheduleConstraintProvider::assignShiftToEmployee)
                .given(employeeAnn, ambulanceShift)
                .penalizesBy(1);
    }
}
