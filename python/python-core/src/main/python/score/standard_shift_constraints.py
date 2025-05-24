from __future__ import annotations

from timefold.solver.score import ConstraintFactory, HardSoftScore, Joiners

from ..domain.shift_scheduling import Shift, Employee, Skill


def no_overlapping_shifts(constraint_factory: ConstraintFactory):
    return (constraint_factory.for_each_unique_pair(
        Shift,
        Joiners.equal(lambda shift: shift.employee),
        Joiners.filtering(lambda shift1, shift2: shift1.timeslot.overlaps(shift2.timeslot))
    )
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("no_overlapping_shifts"))


def employee_availability(constraint_factory: ConstraintFactory):
    return (constraint_factory.for_each(Shift)
            .filter(lambda shift: shift.employee is not None and
                                 any(shift.timeslot.overlaps(unavailable_slot)
                                     for unavailable_slot in shift.employee.unavailable_timeslots))
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("employee_availability"))


def skill_requirement(constraint_factory: ConstraintFactory):
    return (constraint_factory.for_each(Shift)
            .filter(lambda shift: (shift.employee is not None and
                                   shift.required_skill is not None and
                                   shift.required_skill not in shift.employee.skills))
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("skill_requirement"))


def define_standard_shift_constraints(constraint_factory: ConstraintFactory):
    return [
        no_overlapping_shifts(constraint_factory),
        employee_availability(constraint_factory),
        skill_requirement(constraint_factory)
    ]
