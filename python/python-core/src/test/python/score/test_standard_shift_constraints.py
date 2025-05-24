import datetime

from timefold.solver.test import ConstraintVerifier
from timefold.solver.score import HardSoftScore

from .....main.python.domain.shift_scheduling import Timeslot, Skill, Employee, Shift
from .....main.python.score.standard_shift_constraints import define_standard_shift_constraints, \
    no_overlapping_shifts, employee_availability, skill_requirement

# Setup
constraint_verifier = ConstraintVerifier.create(define_standard_shift_constraints,
                                                [Shift, Employee], Shift)

# Test Data
employee1 = Employee(id="e1", name="Employee 1", skills=[Skill(name="Skill1")])
employee2 = Employee(id="e2", name="Employee 2", skills=[Skill(name="Skill2")])

skill1 = Skill(name="AnySkill")

# Test Case 1: Overlapping Shifts for the Same Employee (Penalty)
def test_overlapping_shifts_same_employee():
    timeslot1 = Timeslot(id="ts1", start_datetime=datetime.datetime(2023, 1, 1, 9, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 11, 0))
    timeslot2 = Timeslot(id="ts2", start_datetime=datetime.datetime(2023, 1, 1, 10, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 12, 0))

    shift1 = Shift(id="s1", timeslot=timeslot1, required_skill=skill1, employee=employee1)
    shift2 = Shift(id="s2", timeslot=timeslot2, required_skill=skill1, employee=employee1)

    constraint_verifier.verify_that(no_overlapping_shifts) \
        .given(employee1, shift1, shift2) \
        .penalizes_by(1)

# Test Case 2: No Overlapping Shifts (Same Employee, Different Times) (No Penalty)
def test_non_overlapping_shifts_same_employee():
    # Non-overlapping: S1: 9-10, S2: 10-11 (boundary, not overlapping)
    timeslot1 = Timeslot(id="ts3", start_datetime=datetime.datetime(2023, 1, 1, 9, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 10, 0))
    timeslot2 = Timeslot(id="ts4", start_datetime=datetime.datetime(2023, 1, 1, 10, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 11, 0))
    # Non-overlapping: S1: 9-10, S3: 11-12
    timeslot3 = Timeslot(id="ts5", start_datetime=datetime.datetime(2023, 1, 1, 11, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 12, 0))


    shift1 = Shift(id="s3", timeslot=timeslot1, required_skill=skill1, employee=employee1)
    shift2 = Shift(id="s4", timeslot=timeslot2, required_skill=skill1, employee=employee1)
    shift3 = Shift(id="s5", timeslot=timeslot3, required_skill=skill1, employee=employee1)


    constraint_verifier.verify_that(no_overlapping_shifts) \
        .given(employee1, shift1, shift2) \
        .has_no_violations()

    constraint_verifier.verify_that(no_overlapping_shifts) \
        .given(employee1, shift1, shift3) \
        .has_no_violations()

# Test Case 3: Overlapping Shifts for Different Employees (No Penalty)
def test_overlapping_shifts_different_employees():
    timeslot1 = Timeslot(id="ts6", start_datetime=datetime.datetime(2023, 1, 1, 9, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 11, 0))
    timeslot2 = Timeslot(id="ts7", start_datetime=datetime.datetime(2023, 1, 1, 10, 0),
                         end_datetime=datetime.datetime(2023, 1, 1, 12, 0))

    shift1 = Shift(id="s6", timeslot=timeslot1, required_skill=skill1, employee=employee1)
    shift2 = Shift(id="s7", timeslot=timeslot2, required_skill=skill1, employee=employee2)

    constraint_verifier.verify_that(no_overlapping_shifts) \
        .given(employee1, employee2, shift1, shift2) \
        .has_no_violations()


# --- Tests for employee_availability ---

# Test Case 1: Employee Unavailable (Penalty)
def test_employee_unavailable():
    unavailable_timeslot = Timeslot(id="uts1", start_datetime=datetime.datetime(2023, 1, 2, 10, 0),
                                    end_datetime=datetime.datetime(2023, 1, 2, 12, 0))
    employee_with_unavailability = Employee(id="e3", name="Unavailable Employee",
                                           skills=[Skill(name="AnySkill")],
                                           unavailable_timeslots=[unavailable_timeslot])

    shift_during_unavailability = Shift(id="s8",
                                        timeslot=Timeslot(id="ts8",
                                                          start_datetime=datetime.datetime(2023, 1, 2, 10, 0),
                                                          end_datetime=datetime.datetime(2023, 1, 2, 11, 0)),
                                        required_skill=skill1,
                                        employee=employee_with_unavailability)
    
    overlapping_shift = Shift(id="s11",
                              timeslot=Timeslot(id="ts11",
                                                start_datetime=datetime.datetime(2023, 1, 2, 9, 0), # Shift starts before
                                                end_datetime=datetime.datetime(2023, 1, 2, 11, 0)), # Shift ends during
                              required_skill=skill1,
                              employee=employee_with_unavailability)

    constraint_verifier.verify_that(employee_availability) \
        .given(employee_with_unavailability, shift_during_unavailability) \
        .penalizes_by(1)
    
    constraint_verifier.verify_that(employee_availability) \
        .given(employee_with_unavailability, overlapping_shift) \
        .penalizes_by(1)


# Test Case 2: Employee Available (No Penalty)
def test_employee_available():
    unavailable_timeslot = Timeslot(id="uts2", start_datetime=datetime.datetime(2023, 1, 3, 10, 0),
                                    end_datetime=datetime.datetime(2023, 1, 3, 12, 0))
    employee_with_unavailability = Employee(id="e4", name="Available Employee",
                                           skills=[Skill(name="AnySkill")],
                                           unavailable_timeslots=[unavailable_timeslot])

    shift_outside_unavailability = Shift(id="s9",
                                         timeslot=Timeslot(id="ts9",
                                                           start_datetime=datetime.datetime(2023, 1, 3, 13, 0),
                                                           end_datetime=datetime.datetime(2023, 1, 3, 14, 0)),
                                         required_skill=skill1,
                                         employee=employee_with_unavailability)

    constraint_verifier.verify_that(employee_availability) \
        .given(employee_with_unavailability, shift_outside_unavailability) \
        .has_no_violations()


# Test Case 3: Employee Unavailable but Shift Assigned to Different Employee (No Penalty)
def test_employee_unavailable_different_employee_assigned():
    unavailable_timeslot_e5 = Timeslot(id="uts3", start_datetime=datetime.datetime(2023, 1, 4, 10, 0),
                                       end_datetime=datetime.datetime(2023, 1, 4, 12, 0))
    employee_unavailable = Employee(id="e5", name="Busy Employee",
                                   skills=[Skill(name="AnySkill")],
                                   unavailable_timeslots=[unavailable_timeslot_e5])
    available_employee = Employee(id="e6", name="Free Employee", skills=[Skill(name="AnySkill")])

    shift_during_e5_unavailability = Shift(id="s10",
                                           timeslot=Timeslot(id="ts10",
                                                             start_datetime=datetime.datetime(2023, 1, 4, 10, 0),
                                                             end_datetime=datetime.datetime(2023, 1, 4, 11, 0)),
                                           required_skill=skill1,
                                           employee=available_employee) # Assigned to e6 (available_employee)

    constraint_verifier.verify_that(employee_availability) \
        .given(employee_unavailable, available_employee, shift_during_e5_unavailability) \
        .has_no_violations()


# Test Case 4: Shift With No Employee Assigned (No Penalty)
def test_shift_with_no_employee_assigned_availability():
    unavailable_timeslot_e7 = Timeslot(id="uts4", start_datetime=datetime.datetime(2023, 1, 5, 10, 0),
                                       end_datetime=datetime.datetime(2023, 1, 5, 12, 0))
    employee_with_unavailability = Employee(id="e7", name="Another Busy Employee",
                                           skills=[Skill(name="AnySkill")],
                                           unavailable_timeslots=[unavailable_timeslot_e7])

    shift_with_no_employee = Shift(id="s12",
                                   timeslot=Timeslot(id="ts12",
                                                     start_datetime=datetime.datetime(2023, 1, 5, 10, 0),
                                                     end_datetime=datetime.datetime(2023, 1, 5, 11, 0)),
                                   required_skill=skill1,
                                   employee=None) # No employee assigned

    constraint_verifier.verify_that(employee_availability) \
        .given(employee_with_unavailability, shift_with_no_employee) \
        .has_no_violations()


# --- Tests for skill_requirement ---

# Test Data for Skill Tests
skill_java = Skill(name="Java")
skill_python = Skill(name="Python")
skill_any = Skill(name="AnySkill") # Re-using skill1 for clarity in these tests

default_timeslot = Timeslot(id="ts_skill_default", start_datetime=datetime.datetime(2023, 1, 6, 9, 0),
                            end_datetime=datetime.datetime(2023, 1, 6, 17, 0))

# Test Case 1: Employee Missing Required Skill (Penalty)
def test_employee_missing_required_skill():
    employee_python_only = Employee(id="e8", name="Python Developer", skills=[skill_python])
    shift_requires_java = Shift(id="s13", timeslot=default_timeslot,
                                required_skill=skill_java, employee=employee_python_only)

    constraint_verifier.verify_that(skill_requirement) \
        .given(employee_python_only, shift_requires_java, skill_java, skill_python) \
        .penalizes_by(1)

# Test Case 2: Employee Has Required Skill (No Penalty)
def test_employee_has_required_skill():
    employee_java_dev = Employee(id="e9", name="Java Developer", skills=[skill_java, skill_python])
    shift_requires_java = Shift(id="s14", timeslot=default_timeslot,
                                required_skill=skill_java, employee=employee_java_dev)

    constraint_verifier.verify_that(skill_requirement) \
        .given(employee_java_dev, shift_requires_java, skill_java, skill_python) \
        .has_no_violations()

# Test Case 3: Shift Has No Required Skill (No Penalty)
def test_shift_has_no_required_skill():
    employee_no_java = Employee(id="e10", name="No Java Skill Employee", skills=[skill_python])
    shift_no_skill_needed = Shift(id="s15", timeslot=default_timeslot,
                                  required_skill=None, employee=employee_no_java)

    constraint_verifier.verify_that(skill_requirement) \
        .given(employee_no_java, shift_no_skill_needed, skill_python) \
        .has_no_violations()

# Test Case 4: Shift Has No Employee Assigned (No Penalty)
def test_shift_has_no_employee_assigned_skill():
    employee_irrelevant = Employee(id="e11", name="Irrelevant Employee", skills=[skill_java]) # Skills don't matter here
    shift_requires_java_unassigned = Shift(id="s16", timeslot=default_timeslot,
                                           required_skill=skill_java, employee=None)

    constraint_verifier.verify_that(skill_requirement) \
        .given(employee_irrelevant, shift_requires_java_unassigned, skill_java) \
        .has_no_violations()
