package ai.timefold.solver.examples.nurserostering.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;

class NurseRosteringSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<NurseRoster> {

    @Override
    protected CommonApp<NurseRoster> createCommonApp() {
        return new NurseRosteringApp();
    }

}
