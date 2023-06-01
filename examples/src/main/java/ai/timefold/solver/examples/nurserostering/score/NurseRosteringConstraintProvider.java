package ai.timefold.solver.examples.nurserostering.score;

import static ai.timefold.solver.examples.nurserostering.optional.score.EmployeeConsecutiveAssignmentEnd.getDistanceToLastDayOfWeekend;
import static ai.timefold.solver.examples.nurserostering.optional.score.EmployeeConsecutiveAssignmentEnd.isWeekendAndNotLastDayOfWeekend;
import static ai.timefold.solver.examples.nurserostering.optional.score.EmployeeConsecutiveAssignmentStart.getDistanceToFirstDayOfWeekend;
import static ai.timefold.solver.examples.nurserostering.optional.score.EmployeeConsecutiveAssignmentStart.isWeekendAndNotFirstDayOfWeekend;

import java.time.DayOfWeek;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.examples.common.experimental.ExperimentalConstraintCollectors;
import ai.timefold.solver.examples.common.experimental.api.ConsecutiveInfo;
import ai.timefold.solver.examples.common.util.Pair;
import ai.timefold.solver.examples.nurserostering.domain.Employee;
import ai.timefold.solver.examples.nurserostering.domain.NurseRosterParametrization;
import ai.timefold.solver.examples.nurserostering.domain.ShiftAssignment;
import ai.timefold.solver.examples.nurserostering.domain.ShiftDate;
import ai.timefold.solver.examples.nurserostering.domain.ShiftTypeSkillRequirement;
import ai.timefold.solver.examples.nurserostering.domain.SkillProficiency;
import ai.timefold.solver.examples.nurserostering.domain.contract.BooleanContractLine;
import ai.timefold.solver.examples.nurserostering.domain.contract.ContractLine;
import ai.timefold.solver.examples.nurserostering.domain.contract.ContractLineType;
import ai.timefold.solver.examples.nurserostering.domain.contract.MinMaxContractLine;
import ai.timefold.solver.examples.nurserostering.domain.contract.PatternContractLine;
import ai.timefold.solver.examples.nurserostering.domain.pattern.FreeBefore2DaysWithAWorkDayPattern;
import ai.timefold.solver.examples.nurserostering.domain.pattern.ShiftType2DaysPattern;
import ai.timefold.solver.examples.nurserostering.domain.pattern.ShiftType3DaysPattern;
import ai.timefold.solver.examples.nurserostering.domain.request.DayOffRequest;
import ai.timefold.solver.examples.nurserostering.domain.request.DayOnRequest;
import ai.timefold.solver.examples.nurserostering.domain.request.ShiftOffRequest;
import ai.timefold.solver.examples.nurserostering.domain.request.ShiftOnRequest;

public class NurseRosteringConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                oneShiftPerDay(constraintFactory),
                minimumAndMaximumNumberOfAssignments(constraintFactory),
                minimumNumberOfAssignmentsNoAssignments(constraintFactory),
                consecutiveWorkingDays(constraintFactory),
                consecutiveFreeDays(constraintFactory),
                maximumConsecutiveFreeDaysNoAssignments(constraintFactory),
                consecutiveWorkingWeekends(constraintFactory),
                startOnNotFirstDayOfWeekend(constraintFactory),
                endOnNotLastDayOfWeekend(constraintFactory),
                identicalShiftTypesDuringWeekend(constraintFactory),
                dayOffRequest(constraintFactory),
                dayOnRequest(constraintFactory),
                shiftOffRequest(constraintFactory),
                shiftOnRequest(constraintFactory),
                alternativeSkill(constraintFactory),
                unwantedPatternFreeBefore2DaysWithAWorkDayPattern(constraintFactory),
                unwantedPatternShiftType2DaysPattern(constraintFactory),
                unwantedPatternShiftType3DaysPattern(constraintFactory),
        };
    }

    // ############################################################################
    // Hard constraints
    // ############################################################################

    // A nurse can only work one shift per day, i.e. no two shift can be assigned to the same nurse on a day.
    Constraint oneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(ShiftAssignment.class,
                Joiners.equal(ShiftAssignment::getEmployee),
                Joiners.equal(ShiftAssignment::getShiftDate))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("oneShiftPerDay");
    }

    // ############################################################################
    // Soft constraints
    // ############################################################################
    Constraint minimumAndMaximumNumberOfAssignments(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine.getContractLineType() == ContractLineType.TOTAL_ASSIGNMENTS &&
                        minMaxContractLine.isEnabled())
                .join(constraintFactory.forEach(ShiftAssignment.class)
                        .filter(shift -> shift.getEmployee() != null),
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((line, shift) -> shift.getEmployee(),
                        (line, shift) -> line,
                        ConstraintCollectors.countBi())
                .map((employee, contract, shiftCount) -> employee,
                        (employee, contract, shiftCount) -> contract,
                        (employee, contract, shiftCount) -> contract.getViolationAmount(shiftCount))
                .filter((employee, contract, violationAmount) -> violationAmount != 0)
                .penalize(HardSoftScore.ONE_SOFT, (employee, contract, violationAmount) -> violationAmount)
                .indictWith((employee, contract, violationAmount) -> Arrays.asList(employee, contract))
                .asConstraint("Minimum and maximum number of assignments");
    }

    Constraint minimumNumberOfAssignmentsNoAssignments(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine.getContractLineType() == ContractLineType.TOTAL_ASSIGNMENTS &&
                        minMaxContractLine.isEnabled())
                .join(Employee.class,
                        Joiners.equal(MinMaxContractLine::getContract, Employee::getContract))
                .ifNotExists(ShiftAssignment.class,
                        Joiners.equal((contractLine, employee) -> employee, ShiftAssignment::getEmployee))
                .expand((contract, employee) -> contract.getViolationAmount(0))
                .filter((contract, employee, violationAmount) -> violationAmount != 0)
                .penalize(HardSoftScore.ONE_SOFT, (contract, employee, violationAmount) -> violationAmount)
                .indictWith((contract, employee, violationAmount) -> Arrays.asList(employee, contract))
                .asConstraint("Minimum and maximum number of assignments (no assignments)");
    }

    // Min/Max consecutive working days
    Constraint consecutiveWorkingDays(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine
                        .getContractLineType() == ContractLineType.CONSECUTIVE_WORKING_DAYS &&
                        minMaxContractLine.isEnabled())
                .join(ShiftAssignment.class,
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((contract, shift) -> shift.getEmployee(),
                        (contract, shift) -> contract,
                        ExperimentalConstraintCollectors.consecutive((contract, shift) -> shift.getShiftDate(),
                                ShiftDate::getDayIndex))
                .flattenLast(ConsecutiveInfo::getConsecutiveSequences)
                .map((employee, contract, shiftList) -> employee,
                        (employee, contract, shiftList) -> contract,
                        (employee, contract, shiftList) -> contract.getViolationAmount(shiftList.getLength()))
                .filter((contract, employee, violationAmount) -> violationAmount != 0)
                .penalize(HardSoftScore.ONE_SOFT, (contract, employee, violationAmount) -> violationAmount)
                .indictWith((contract, employee, violationAmount) -> Arrays.asList(employee, contract))
                .asConstraint("consecutiveWorkingDays");
    }

    // Min/Max consecutive free days
    Constraint consecutiveFreeDays(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine
                        .getContractLineType() == ContractLineType.CONSECUTIVE_FREE_DAYS &&
                        minMaxContractLine.isEnabled())
                .join(ShiftAssignment.class,
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((contract, shift) -> shift.getEmployee(),
                        (contract, shift) -> contract,
                        ExperimentalConstraintCollectors.consecutive((contract, shift) -> shift.getShiftDate(),
                                ShiftDate::getDayIndex))
                .flattenLast(ConsecutiveInfo::getConsecutiveSequences)
                .join(NurseRosterParametrization.class)
                .map((employee, contract, shiftSequence, nrp) -> employee,
                        (employee, contract, shiftSequence, nrp) -> contract,
                        (employee, contract, shiftSequence, nrp) -> {
                            // Use NurseRosterParametrization to compute and cache violations.
                            int total = 0;
                            if (!shiftSequence.isFirst()
                                    && contract.isViolated(shiftSequence.getPreviousBreak().getLength() - 1)) {
                                total += contract.getViolationAmount(shiftSequence.getPreviousBreak().getLength() - 1);
                            }
                            if (shiftSequence.isFirst()) {
                                int length = shiftSequence.getFirstItem().getDayIndex() - nrp.getFirstShiftDateDayIndex();
                                if (length > 0 && contract.isViolated(length)) {
                                    total += contract.getViolationAmount(length);
                                }
                            }
                            if (shiftSequence.isLast()) {
                                int length = nrp.getLastShiftDateDayIndex() - shiftSequence.getLastItem().getDayIndex();
                                if (length > 0 && contract.isViolated(length)) {
                                    total += contract.getViolationAmount(length);
                                }
                            }
                            return total;
                        })
                .filter((employee, contract, violationAmount) -> violationAmount != 0)
                .penalize(HardSoftScore.ONE_SOFT, (employee, contract, violationAmount) -> violationAmount)
                .indictWith((employee, contract, violationAmount) -> Arrays.asList(employee, contract))
                .asConstraint("consecutiveFreeDays");
    }

    Constraint maximumConsecutiveFreeDaysNoAssignments(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine
                        .getContractLineType() == ContractLineType.CONSECUTIVE_FREE_DAYS &&
                        minMaxContractLine.isMaximumEnabled())
                .join(Employee.class,
                        Joiners.equal(MinMaxContractLine::getContract, Employee::getContract))
                .ifNotExists(ShiftAssignment.class,
                        Joiners.equal((contract, employee) -> employee, ShiftAssignment::getEmployee))
                .join(NurseRosterParametrization.class,
                        Joiners.lessThan((contract, employee) -> contract.getMaximumValue(),
                                nrp -> nrp.getLastShiftDateDayIndex() - nrp.getFirstShiftDateDayIndex() + 1))
                .penalize(HardSoftScore.ONE_SOFT,
                        (contract, employee, nrp) -> contract
                                .getViolationAmount(nrp.getLastShiftDateDayIndex() - nrp.getFirstShiftDateDayIndex() + 1))
                .indictWith((contract, employee, nrp) -> Arrays.asList(employee, contract))
                .asConstraint("maximumConsecutiveFreeDays (no shifts)");
    }

    // Min/Max consecutive working weekends
    Constraint consecutiveWorkingWeekends(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(MinMaxContractLine.class)
                .filter(minMaxContractLine -> minMaxContractLine
                        .getContractLineType() == ContractLineType.CONSECUTIVE_WORKING_WEEKENDS &&
                        minMaxContractLine.isEnabled())
                .join(constraintFactory.forEach(ShiftAssignment.class)
                        .filter(ShiftAssignment::isWeekend),
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((contract, shift) -> shift.getEmployee(),
                        (contract, shift) -> contract,
                        ExperimentalConstraintCollectors.consecutive((contract, shift) -> shift.getShiftDate(),
                                shiftDate -> shiftDate.getWeekendSundayIndex() / 7))
                .flattenLast(ConsecutiveInfo::getConsecutiveSequences)
                .map((employee, contract, shiftList) -> employee,
                        (employee, contract, shiftList) -> contract,
                        (employee, contract, shiftList) -> contract.getViolationAmount(shiftList.getLength()))
                .filter((employee, contract, violationAmount) -> violationAmount != 0)
                .penalize(HardSoftScore.ONE_SOFT, (employee, contract, violationAmount) -> violationAmount)
                .indictWith((employee, contract, violationAmount) -> Arrays.asList(employee, contract))
                .asConstraint("consecutiveWorkingWeekends");
    }

    // Complete Weekends
    Constraint startOnNotFirstDayOfWeekend(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BooleanContractLine.class)
                .filter(booleanContractLine -> booleanContractLine.getContractLineType() == ContractLineType.COMPLETE_WEEKENDS
                        && booleanContractLine.isEnabled())
                .join(ShiftAssignment.class,
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((contract, shift) -> shift.getEmployee(),
                        (contract, shift) -> contract,
                        ExperimentalConstraintCollectors.consecutive((contract, shift) -> shift.getShiftDate(),
                                ShiftDate::getDayIndex))
                .flattenLast(ConsecutiveInfo::getConsecutiveSequences)
                .filter((employee, contract, shiftList) -> isWeekendAndNotFirstDayOfWeekend(employee,
                        shiftList.getFirstItem()))
                .penalize(HardSoftScore.ONE_SOFT,
                        (employee, contract, shiftList) -> getDistanceToFirstDayOfWeekend(employee, shiftList.getFirstItem())
                                * contract.getWeight())
                .indictWith((employee, contract, shiftList) -> Arrays.asList(employee, contract))
                .asConstraint("startOnNotFirstDayOfWeekend");
    }

    Constraint endOnNotLastDayOfWeekend(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BooleanContractLine.class)
                .filter(booleanContractLine -> booleanContractLine
                        .getContractLineType() == ContractLineType.COMPLETE_WEEKENDS &&
                        booleanContractLine.isEnabled())
                .join(ShiftAssignment.class,
                        Joiners.equal(ContractLine::getContract, ShiftAssignment::getContract))
                .groupBy((contract, shift) -> shift.getEmployee(),
                        (contract, shift) -> contract,
                        ExperimentalConstraintCollectors.consecutive((contract, shift) -> shift.getShiftDate(),
                                ShiftDate::getDayIndex))
                .flattenLast(ConsecutiveInfo::getConsecutiveSequences)
                .filter((employee, contract, shiftList) -> isWeekendAndNotLastDayOfWeekend(employee,
                        shiftList.getLastItem()))
                .penalize(HardSoftScore.ONE_SOFT,
                        (employee, contract, shiftList) -> getDistanceToLastDayOfWeekend(employee, shiftList.getLastItem())
                                * contract.getWeight())
                .indictWith((employee, contract, shiftList) -> Arrays.asList(employee, contract))
                .asConstraint("endOnNotLastDayOfWeekend");
    }

    // Identical shiftTypes during a weekend
    Constraint identicalShiftTypesDuringWeekend(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BooleanContractLine.class)
                .filter(booleanContractLine -> booleanContractLine
                        .getContractLineType() == ContractLineType.IDENTICAL_SHIFT_TYPES_DURING_WEEKEND &&
                        booleanContractLine.isEnabled())
                .join(constraintFactory.forEach(ShiftDate.class)
                        .filter(date -> date.getDayOfWeek() == DayOfWeek.SUNDAY))
                .join(constraintFactory.forEach(ShiftAssignment.class)
                        .filter(ShiftAssignment::isWeekend),
                        Joiners.equal((contract, date) -> date.getWeekendSundayIndex(), ShiftAssignment::getWeekendSundayIndex),
                        Joiners.equal((contract, date) -> contract.getContract(), ShiftAssignment::getContract))
                .groupBy((contract, date, sa) -> contract,
                        (contract, date, sa) -> sa.getEmployee(),
                        (contract, date, sa) -> Pair.of(sa.getShiftType(), date), // No 4-key groupBy overload
                        ConstraintCollectors.countTri())
                .filter((contract, employee, type, count) -> count < employee.getWeekendLength())
                .penalize(HardSoftScore.ONE_SOFT,
                        (contract, employee, type, count) -> (employee.getWeekendLength() - count) * contract.getWeight())
                .indictWith((contract, employee, type, count) -> Arrays.asList(employee, contract))
                .asConstraint("identicalShiftTypesDuringWeekend");
    }

    // Requested day on/off
    Constraint dayOffRequest(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(DayOffRequest.class)
                .join(ShiftAssignment.class, Joiners.equal(DayOffRequest::getEmployee, ShiftAssignment::getEmployee),
                        Joiners.equal(DayOffRequest::getShiftDate, ShiftAssignment::getShiftDate))
                .penalize(HardSoftScore.ONE_SOFT,
                        (dayOffRequest, shiftAssignment) -> dayOffRequest.getWeight())
                .asConstraint("dayOffRequest");
    }

    Constraint dayOnRequest(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(DayOnRequest.class)
                .ifNotExists(ShiftAssignment.class, Joiners.equal(DayOnRequest::getEmployee, ShiftAssignment::getEmployee),
                        Joiners.equal(DayOnRequest::getShiftDate, ShiftAssignment::getShiftDate))
                .penalize(HardSoftScore.ONE_SOFT, DayOnRequest::getWeight)
                .asConstraint("dayOnRequest");
    }

    // Requested shift on/off
    Constraint shiftOffRequest(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftOffRequest.class)
                .join(ShiftAssignment.class, Joiners.equal(ShiftOffRequest::getEmployee, ShiftAssignment::getEmployee),
                        Joiners.equal(ShiftOffRequest::getShift, ShiftAssignment::getShift))
                .penalize(HardSoftScore.ONE_SOFT,
                        (shiftOffRequest, shiftAssignment) -> shiftOffRequest.getWeight())
                .asConstraint("shiftOffRequest");
    }

    Constraint shiftOnRequest(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftOnRequest.class)
                .ifNotExists(ShiftAssignment.class, Joiners.equal(ShiftOnRequest::getEmployee, ShiftAssignment::getEmployee),
                        Joiners.equal(ShiftOnRequest::getShift, ShiftAssignment::getShift))
                .penalize(HardSoftScore.ONE_SOFT, ShiftOnRequest::getWeight)
                .asConstraint("shiftOnRequest");
    }

    // Alternative skill
    Constraint alternativeSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(BooleanContractLine.class)
                .filter(booleanContractLine -> booleanContractLine.getContractLineType()
                        .equals(ContractLineType.ALTERNATIVE_SKILL_CATEGORY))
                .join(ShiftAssignment.class, Joiners.equal(BooleanContractLine::getContract, ShiftAssignment::getContract))
                .join(ShiftTypeSkillRequirement.class,
                        Joiners.equal((contract, shiftAssignment) -> shiftAssignment.getShiftType(),
                                ShiftTypeSkillRequirement::getShiftType))
                .ifNotExists(SkillProficiency.class,
                        Joiners.equal((contract, shiftAssignment, skillRequirement) -> shiftAssignment.getEmployee(),
                                SkillProficiency::getEmployee),
                        Joiners.equal((contract, shiftAssignment, skillRequirement) -> skillRequirement.getSkill(),
                                SkillProficiency::getSkill))
                .penalize(HardSoftScore.ONE_SOFT,
                        (contractLine, shiftAssignment, skillRequirement) -> contractLine.getWeight())
                .asConstraint("alternativeSkill");
    }

    // Unwanted patterns
    Constraint unwantedPatternFreeBefore2DaysWithAWorkDayPattern(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(PatternContractLine.class)
                .filter(patternContractLine -> patternContractLine.getPattern() instanceof FreeBefore2DaysWithAWorkDayPattern)
                .join(ShiftDate.class,
                        Joiners.equal(
                                contract -> ((FreeBefore2DaysWithAWorkDayPattern) contract.getPattern()).getFreeDayOfWeek(),
                                ShiftDate::getDayOfWeek))
                .join(Employee.class,
                        Joiners.equal((contractLine, date) -> contractLine.getContract(), Employee::getContract))
                .ifNotExists(ShiftAssignment.class,
                        Joiners.equal((contractLine, date, employee) -> employee, ShiftAssignment::getEmployee),
                        Joiners.equal((contractLine, date, employee) -> date.getDayIndex(),
                                ShiftAssignment::getShiftDateDayIndex))
                .ifExists(ShiftAssignment.class,
                        Joiners.equal((contractLine, date, employee) -> employee, ShiftAssignment::getEmployee),
                        Joiners.lessThanOrEqual((contractLine, date, employee) -> date.getDayIndex() + 1,
                                ShiftAssignment::getShiftDateDayIndex),
                        Joiners.greaterThanOrEqual((contractLine, date, employee) -> date.getDayIndex() + 2,
                                ShiftAssignment::getShiftDateDayIndex))
                .penalize(HardSoftScore.ONE_SOFT,
                        (contractLine, date, employee) -> contractLine.getPattern().getWeight())
                .asConstraint("unwantedPatternFreeBefore2DaysWithAWorkDayPattern");
    }

    Constraint unwantedPatternShiftType2DaysPattern(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(PatternContractLine.class)
                .filter(patternContractLine -> patternContractLine.getPattern() instanceof ShiftType2DaysPattern)
                .join(ShiftAssignment.class,
                        Joiners.equal(
                                contractLine -> ((ShiftType2DaysPattern) contractLine.getPattern()).getDayIndex0ShiftType(),
                                ShiftAssignment::getShiftType),
                        Joiners.equal(PatternContractLine::getContract, ShiftAssignment::getContract))
                .join(ShiftAssignment.class,
                        Joiners.equal((contractLine, shift) -> shift.getEmployee(), ShiftAssignment::getEmployee),
                        Joiners.equal((contractLine, shift) -> shift.getShiftDateDayIndex() + 1,
                                ShiftAssignment::getShiftDateDayIndex),
                        Joiners.filtering((contractLine, shift1, shift2) -> {
                            ShiftType2DaysPattern pattern = (ShiftType2DaysPattern) contractLine.getPattern();
                            return pattern.getDayIndex1ShiftType() == null
                                    || shift2.getShiftType() == pattern.getDayIndex1ShiftType();
                        }))
                .penalize(HardSoftScore.ONE_SOFT,
                        (contractLine, shift1, shift2) -> contractLine.getPattern().getWeight())
                .asConstraint("unwantedPatternShiftType2DaysPattern");
    }

    Constraint unwantedPatternShiftType3DaysPattern(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(PatternContractLine.class) // ShiftType3DaysPattern
                .filter(patternContractLine -> patternContractLine.getPattern() instanceof ShiftType3DaysPattern)
                .join(ShiftAssignment.class,
                        Joiners.equal(
                                contractLine -> ((ShiftType3DaysPattern) contractLine.getPattern()).getDayIndex0ShiftType(),
                                ShiftAssignment::getShiftType),
                        Joiners.equal(PatternContractLine::getContract, ShiftAssignment::getContract))
                // Join and not if exist for consistency with DRL (which is removed)
                .join(ShiftAssignment.class,
                        Joiners.equal((contractLine, shift) -> shift.getEmployee(), ShiftAssignment::getEmployee),
                        Joiners.equal((contractLine, shift) -> shift.getShiftDateDayIndex() + 1,
                                ShiftAssignment::getShiftDateDayIndex),
                        Joiners.equal(
                                (contractLine, shift) -> ((ShiftType3DaysPattern) contractLine.getPattern())
                                        .getDayIndex1ShiftType(),
                                ShiftAssignment::getShiftType))
                .join(ShiftAssignment.class,
                        Joiners.equal((contractLine, shift1, shift2) -> shift1.getEmployee(),
                                ShiftAssignment::getEmployee),
                        Joiners.equal((contractLine, shift1, shift2) -> shift1.getShiftDateDayIndex() + 2,
                                ShiftAssignment::getShiftDateDayIndex),
                        Joiners.equal(
                                (contractLine, shift1, shift2) -> ((ShiftType3DaysPattern) contractLine.getPattern())
                                        .getDayIndex2ShiftType(),
                                ShiftAssignment::getShiftType))
                .penalize(HardSoftScore.ONE_SOFT,
                        (contractLine, shift1, shift2, shift3) -> contractLine.getPattern().getWeight())
                .asConstraint("unwantedPatternShiftType3DaysPattern");
    }
}
