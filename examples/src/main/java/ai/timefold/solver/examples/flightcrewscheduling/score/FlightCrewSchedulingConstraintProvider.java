package ai.timefold.solver.examples.flightcrewscheduling.score;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.overlapping;

import java.time.LocalDate;
import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Employee;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightAssignment;
import ai.timefold.solver.examples.flightcrewscheduling.domain.Skill;

public class FlightCrewSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                requiredSkill(constraintFactory),
                flightConflict(constraintFactory),
                transferBetweenTwoFlights(constraintFactory),
                employeeUnavailability(constraintFactory),
                firstAssignmentNotDepartingFromHome(constraintFactory),
                lastAssignmentNotArrivingAtHome(constraintFactory)
        };
    }

    private Constraint requiredSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(FlightAssignment.class)
                .filter(flightAssignment -> {
                    Skill requiredSkill = flightAssignment.getRequiredSkill();
                    return !flightAssignment.getEmployee().hasSkill(requiredSkill);
                })
                .penalize(HardSoftLongScore.ofHard(100))
                .asConstraint("Required skill");
    }

    private Constraint flightConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(FlightAssignment.class, equal(FlightAssignment::getEmployee),
                overlapping(flightAssignment -> flightAssignment.getFlight().getDepartureUTCDateTime(),
                        flightAssignment -> flightAssignment.getFlight().getArrivalUTCDateTime()))
                .penalize(HardSoftLongScore.ofHard(10))
                .asConstraint("Flight conflict");
    }

    private Constraint transferBetweenTwoFlights(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                .expand(Employee::countInvalidConnections)
                .filter((employee, invalidConnections) -> invalidConnections > 0)
                .penalizeLong(HardSoftLongScore.ofHard(1),
                        (employee, invalidConnections) -> invalidConnections)
                .indictWith((employee, invalidConnections) -> Collections.singleton(employee))
                .asConstraint("Transfer between two flights");
    }

    private Constraint employeeUnavailability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(FlightAssignment.class)
                .filter(flightAssignment -> {
                    LocalDate departureUTCDate = flightAssignment.getFlight().getDepartureUTCDate();
                    return !flightAssignment.getEmployee().isAvailable(departureUTCDate);
                })
                .penalize(HardSoftLongScore.ofHard(10))
                .asConstraint("Employee unavailable");
    }

    private Constraint firstAssignmentNotDepartingFromHome(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                .filter(employee -> !employee.isFirstAssignmentDepartingFromHome())
                .penalize(HardSoftLongScore.ofSoft(1_000_000))
                .asConstraint("First assignment not departing from home");
    }

    private Constraint lastAssignmentNotArrivingAtHome(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                .filter(employee -> !employee.isLastAssignmentArrivingAtHome())
                .penalize(HardSoftLongScore.ofSoft(1_000_000))
                .asConstraint("Last assignment not arriving at home");
    }

}
