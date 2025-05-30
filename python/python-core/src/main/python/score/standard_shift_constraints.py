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


def filter_cannot_work_together(shift1: Shift, shift2: Shift) -> bool:
    if shift1.employee is None or shift2.employee is None:
        return False
    # If employees are the same, this is not a "cannot work together" scenario,
    # but potentially an "overlapping shift" for the same employee, handled by another constraint.
    if shift1.employee.id == shift2.employee.id:
        return False
    if not shift1.timeslot.overlaps(shift2.timeslot):
        return False
    if shift1.employee.id in shift2.employee.cannot_work_with:
        return True
    if shift2.employee.id in shift1.employee.cannot_work_with:
        return True
    return False


def cannot_work_together(constraint_factory: ConstraintFactory):
    return (constraint_factory.for_each_unique_pair(
        Shift,
        Joiners.filtering(filter_cannot_work_together)
    )
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("cannot_work_together"))


def define_standard_shift_constraints(constraint_factory: ConstraintFactory):
    return [
        no_overlapping_shifts(constraint_factory),
        employee_availability(constraint_factory),
        skill_requirement(constraint_factory),
        cannot_work_together(constraint_factory),
        seniority_requirement(constraint_factory)
    ]


def seniority_requirement(constraint_factory: ConstraintFactory):
    return (constraint_factory.for_each(Shift)
            .filter(lambda s_junior: (s_junior.employee is not None and
                                      s_junior.employee.classification in ["nurse", "paramedic"] and
                                      not s_junior.employee.is_senior))
            .if_not_exists(Shift,
                           Joiners.filtering(lambda s_junior, s_other: s_junior.id != s_other.id),
                           Joiners.filtering(lambda s_junior, s_other: (s_other.employee is not None and
                                                                       s_other.employee.classification in ["nurse", "paramedic"])),
                           Joiners.filtering(lambda s_junior, s_other: s_junior.timeslot.overlaps(s_other.timeslot))
                           )
            .penalize(HardSoftScore.ONE_HARD)
            .as_constraint("seniority_requirement"))
