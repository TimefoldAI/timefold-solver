package ai.timefold.solver.examples.projectjobscheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

class ProjectJobSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<Schedule> {

    @Override
    protected CommonApp<Schedule> createCommonApp() {
        return new ProjectJobSchedulingApp();
    }

}
