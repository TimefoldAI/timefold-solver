package ai.timefold.solver.examples.taskassigning.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.taskassigning.domain.TaskAssigningSolution;

class TaskAssigningSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<TaskAssigningSolution> {

    @Override
    protected CommonApp<TaskAssigningSolution> createCommonApp() {
        return new TaskAssigningApp();
    }
}
