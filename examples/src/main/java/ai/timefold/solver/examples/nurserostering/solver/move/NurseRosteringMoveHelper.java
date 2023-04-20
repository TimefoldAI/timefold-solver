package ai.timefold.solver.examples.nurserostering.solver.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.examples.nurserostering.domain.Employee;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.domain.ShiftAssignment;

public class NurseRosteringMoveHelper {

    public static void moveEmployee(ScoreDirector<NurseRoster> scoreDirector, ShiftAssignment shiftAssignment,
            Employee toEmployee) {
        scoreDirector.beforeVariableChanged(shiftAssignment, "employee");
        shiftAssignment.setEmployee(toEmployee);
        scoreDirector.afterVariableChanged(shiftAssignment, "employee");
    }

    private NurseRosteringMoveHelper() {
    }

}
