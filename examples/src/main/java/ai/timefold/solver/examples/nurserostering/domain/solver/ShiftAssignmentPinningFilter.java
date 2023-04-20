package ai.timefold.solver.examples.nurserostering.domain.solver;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.domain.ShiftAssignment;
import ai.timefold.solver.examples.nurserostering.domain.ShiftDate;

public class ShiftAssignmentPinningFilter implements PinningFilter<NurseRoster, ShiftAssignment> {

    @Override
    public boolean accept(NurseRoster nurseRoster, ShiftAssignment shiftAssignment) {
        ShiftDate shiftDate = shiftAssignment.getShift().getShiftDate();
        return !nurseRoster.getNurseRosterParametrization().isInPlanningWindow(shiftDate);
    }

}
