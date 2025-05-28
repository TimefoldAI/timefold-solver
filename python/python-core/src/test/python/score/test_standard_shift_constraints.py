import datetime

from timefold.solver.test import ConstraintVerifier
from timefold.solver.score import HardSoftScore

from .....main.python.domain.shift_scheduling import Timeslot, Skill, Employee, Shift
from .....main.python.score.standard_shift_constraints import define_standard_shift_constraints, \
    no_overlapping_shifts, employee_availability, skill_requirement, cannot_work_together, seniority_requirement

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


# --- Tests for cannot_work_together ---

# Common Timeslots for CWT tests
cwt_ts_overlap1 = Timeslot(id="cwt_ts1", start_datetime=datetime.datetime(2023, 2, 1, 9, 0),
                           end_datetime=datetime.datetime(2023, 2, 1, 11, 0))
cwt_ts_overlap2 = Timeslot(id="cwt_ts2", start_datetime=datetime.datetime(2023, 2, 1, 10, 0),
                           end_datetime=datetime.datetime(2023, 2, 1, 12, 0))
cwt_ts_non_overlap1 = Timeslot(id="cwt_ts3", start_datetime=datetime.datetime(2023, 2, 1, 13, 0),
                               end_datetime=datetime.datetime(2023, 2, 1, 14, 0))
cwt_ts_non_overlap2 = Timeslot(id="cwt_ts4", start_datetime=datetime.datetime(2023, 2, 1, 15, 0),
                               end_datetime=datetime.datetime(2023, 2, 1, 16, 0))
cwt_skill = Skill(name="CWT_Skill")


# Test Case 1: Cannot Work Together, Overlapping (Penalty)
def test_cannot_work_together_overlapping_penalty():
    emp_cwt1 = Employee(id="emp_cwt1", name="CWT Emp1", cannot_work_with=["emp_cwt2"])
    emp_cwt2 = Employee(id="emp_cwt2", name="CWT Emp2")

    shift_cwt1 = Shift(id="shift_cwt1", timeslot=cwt_ts_overlap1, required_skill=cwt_skill, employee=emp_cwt1)
    shift_cwt2 = Shift(id="shift_cwt2", timeslot=cwt_ts_overlap2, required_skill=cwt_skill, employee=emp_cwt2)

    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt1, emp_cwt2, shift_cwt1, shift_cwt2) \
        .penalizes_by(1)

    # Test the other direction too
    emp_cwt3 = Employee(id="emp_cwt3", name="CWT Emp3")
    emp_cwt4 = Employee(id="emp_cwt4", name="CWT Emp4", cannot_work_with=["emp_cwt3"])

    shift_cwt3 = Shift(id="shift_cwt3", timeslot=cwt_ts_overlap1, required_skill=cwt_skill, employee=emp_cwt3)
    shift_cwt4 = Shift(id="shift_cwt4", timeslot=cwt_ts_overlap2, required_skill=cwt_skill, employee=emp_cwt4)
    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt3, emp_cwt4, shift_cwt3, shift_cwt4) \
        .penalizes_by(1)


# Test Case 2: Cannot Work Together, Non-Overlapping (No Penalty)
def test_cannot_work_together_non_overlapping_no_penalty():
    emp_cwt_non1 = Employee(id="emp_cwt_non1", name="CWT NonOverlap1", cannot_work_with=["emp_cwt_non2"])
    emp_cwt_non2 = Employee(id="emp_cwt_non2", name="CWT NonOverlap2")

    shift_cwt_non1 = Shift(id="shift_cwt_non1", timeslot=cwt_ts_non_overlap1, required_skill=cwt_skill, employee=emp_cwt_non1)
    shift_cwt_non2 = Shift(id="shift_cwt_non2", timeslot=cwt_ts_non_overlap2, required_skill=cwt_skill, employee=emp_cwt_non2)

    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt_non1, emp_cwt_non2, shift_cwt_non1, shift_cwt_non2) \
        .has_no_violations()

# Test Case 3: Can Work Together, Overlapping (No Penalty)
def test_can_work_together_overlapping_no_penalty():
    emp_cwt_can1 = Employee(id="emp_cwt_can1", name="CWT Can1") # cannot_work_with is empty
    emp_cwt_can2 = Employee(id="emp_cwt_can2", name="CWT Can2")

    shift_cwt_can1 = Shift(id="shift_cwt_can1", timeslot=cwt_ts_overlap1, required_skill=cwt_skill, employee=emp_cwt_can1)
    shift_cwt_can2 = Shift(id="shift_cwt_can2", timeslot=cwt_ts_overlap2, required_skill=cwt_skill, employee=emp_cwt_can2)

    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt_can1, emp_cwt_can2, shift_cwt_can1, shift_cwt_can2) \
        .has_no_violations()

# Test Case 4: One Employee Not Assigned (No Penalty)
def test_cannot_work_together_one_employee_not_assigned():
    emp_cwt_assigned = Employee(id="emp_cwt_assigned", name="CWT Assigned", cannot_work_with=["emp_cwt_unassigned_ghost"])
    emp_cwt_unassigned = Employee(id="emp_cwt_unassigned", name="CWT Unassigned") # This employee exists but is not assigned to shift_cwt_unassigned

    shift_cwt_assigned = Shift(id="shift_cwt_assigned", timeslot=cwt_ts_overlap1, required_skill=cwt_skill, employee=emp_cwt_assigned)
    shift_cwt_unassigned = Shift(id="shift_cwt_unassigned", timeslot=cwt_ts_overlap2, required_skill=cwt_skill, employee=None) # No employee

    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt_assigned, emp_cwt_unassigned, shift_cwt_assigned, shift_cwt_unassigned) \
        .has_no_violations()

    # Also test if the first shift has no employee
    emp_cwt_assigned2 = Employee(id="emp_cwt_assigned2", name="CWT Assigned2")
    emp_cwt_unassigned_ghost2 = Employee(id="emp_cwt_unassigned_ghost2", name="CWT Unassigned Ghost2", cannot_work_with=["emp_cwt_assigned2"])


    shift_cwt_assigned_other = Shift(id="shift_cwt_assigned_other", timeslot=cwt_ts_overlap2, required_skill=cwt_skill, employee=emp_cwt_assigned2)
    shift_cwt_unassigned_first = Shift(id="shift_cwt_unassigned_first", timeslot=cwt_ts_overlap1, required_skill=cwt_skill, employee=None)

    constraint_verifier.verify_that(cannot_work_together) \
        .given(emp_cwt_assigned2, emp_cwt_unassigned_ghost2, shift_cwt_unassigned_first, shift_cwt_assigned_other) \
        .has_no_violations()


# --- Tests for seniority_requirement ---

# Helper for creating employees for seniority tests
def _create_test_employee(id: str, classification: str, is_senior: bool) -> Employee:
    return Employee(id=id, name=f"Test Employee {id}", classification=classification, is_senior=is_senior,
                    skills=[Skill(name="BasicCare")]) # Add a default skill

# Common Timeslots & Skill for Seniority tests
sr_ts1 = Timeslot(id="sr_ts1", start_datetime=datetime.datetime(2023, 3, 1, 9, 0),
                  end_datetime=datetime.datetime(2023, 3, 1, 10, 0))
sr_ts_overlap = Timeslot(id="sr_ts_overlap", start_datetime=datetime.datetime(2023, 3, 1, 9, 30), # Overlaps with sr_ts1
                         end_datetime=datetime.datetime(2023, 3, 1, 10, 30))
sr_skill = Skill(name="SR_Skill")


# Test Case 1: Junior Nurse, Truly Alone (Penalty)
def test_junior_nurse_truly_alone_penalty():
    emp1 = _create_test_employee(id="E1_sr1", classification="nurse", is_senior=False)
    shift1 = Shift(id="shift_sr1", timeslot=sr_ts1, required_skill=sr_skill, employee=emp1)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp1, shift1) \
        .penalizes_by(1)

# Test Case 2: Senior Nurse, Truly Alone (No Penalty)
def test_senior_nurse_truly_alone_no_penalty():
    emp1 = _create_test_employee(id="E1_sr2", classification="nurse", is_senior=True)
    shift1 = Shift(id="shift_sr2", timeslot=sr_ts1, required_skill=sr_skill, employee=emp1)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp1, shift1) \
        .has_no_violations()

# Test Case 3: Junior Nurse, Concurrent Senior Nurse (No Penalty for Junior)
def test_junior_nurse_with_concurrent_senior_nurse_no_penalty():
    emp_junior = _create_test_employee(id="E1_sr3", classification="nurse", is_senior=False)
    emp_senior = _create_test_employee(id="E2_sr3", classification="nurse", is_senior=True)
    shift_junior = Shift(id="shift_sr3_junior", timeslot=sr_ts1, required_skill=sr_skill, employee=emp_junior)
    shift_senior = Shift(id="shift_sr3_senior", timeslot=sr_ts_overlap, required_skill=sr_skill, employee=emp_senior)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp_junior, emp_senior, shift_junior, shift_senior) \
        .has_no_violations()

# Test Case 4: Junior Nurse, Concurrent Junior Nurse (No Penalty for s_junior)
def test_junior_nurse_with_concurrent_junior_nurse_no_penalty():
    emp_junior1 = _create_test_employee(id="E1_sr4", classification="nurse", is_senior=False)
    emp_junior2 = _create_test_employee(id="E2_sr4", classification="nurse", is_senior=False)
    shift_junior1 = Shift(id="shift_sr4_junior1", timeslot=sr_ts1, required_skill=sr_skill, employee=emp_junior1)
    shift_junior2 = Shift(id="shift_sr4_junior2", timeslot=sr_ts_overlap, required_skill=sr_skill, employee=emp_junior2)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp_junior1, emp_junior2, shift_junior1, shift_junior2) \
        .has_no_violations()


# Test Case 5: Junior Paramedic, Truly Alone (Penalty)
def test_junior_paramedic_truly_alone_penalty():
    emp1 = _create_test_employee(id="E1_sr5", classification="paramedic", is_senior=False)
    shift1 = Shift(id="shift_sr5", timeslot=sr_ts1, required_skill=sr_skill, employee=emp1)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp1, shift1) \
        .penalizes_by(1)


# Test Case 6: Junior Nurse, Concurrent Non-Nurse/Paramedic (Penalty for Junior)
def test_junior_nurse_with_concurrent_admin_penalty():
    emp_junior_nurse = _create_test_employee(id="E1_sr6", classification="nurse", is_senior=False)
    emp_admin = _create_test_employee(id="E2_sr6", classification="admin", is_senior=False) # is_senior for admin doesn't matter
    shift_junior_nurse = Shift(id="shift_sr6_nurse", timeslot=sr_ts1, required_skill=sr_skill, employee=emp_junior_nurse)
    shift_admin = Shift(id="shift_sr6_admin", timeslot=sr_ts_overlap, required_skill=sr_skill, employee=emp_admin)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp_junior_nurse, emp_admin, shift_junior_nurse, shift_admin) \
        .penalizes_by(1)


# Test Case 7: Non-Nurse/Paramedic Employee Alone (No Penalty)
def test_admin_alone_no_penalty():
    emp_admin = _create_test_employee(id="E1_sr7", classification="admin", is_senior=False)
    shift_admin = Shift(id="shift_sr7", timeslot=sr_ts1, required_skill=sr_skill, employee=emp_admin)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp_admin, shift_admin) \
        .has_no_violations()

# Test Case 8: Junior Nurse, Shift Unassigned (No Penalty)
def test_junior_nurse_shift_unassigned_no_penalty():
    # This employee exists but is not assigned to the shift
    emp_junior_nurse_exists = _create_test_employee(id="E1_sr8", classification="nurse", is_senior=False)
    shift_unassigned = Shift(id="shift_sr8_unassigned", timeslot=sr_ts1, required_skill=sr_skill, employee=None)

    constraint_verifier.verify_that(seniority_requirement) \
        .given(emp_junior_nurse_exists, shift_unassigned) \
        .has_no_violations()
