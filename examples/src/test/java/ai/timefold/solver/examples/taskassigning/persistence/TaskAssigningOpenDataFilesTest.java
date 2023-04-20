package ai.timefold.solver.examples.taskassigning.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.taskassigning.app.TaskAssigningApp;
import ai.timefold.solver.examples.taskassigning.domain.TaskAssigningSolution;

class TaskAssigningOpenDataFilesTest extends OpenDataFilesTest<TaskAssigningSolution> {

    @Override
    protected CommonApp<TaskAssigningSolution> createCommonApp() {
        return new TaskAssigningApp();
    }
}
