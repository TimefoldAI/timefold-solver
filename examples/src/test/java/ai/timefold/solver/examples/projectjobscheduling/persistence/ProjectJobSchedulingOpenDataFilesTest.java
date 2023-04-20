package ai.timefold.solver.examples.projectjobscheduling.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.projectjobscheduling.app.ProjectJobSchedulingApp;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

class ProjectJobSchedulingOpenDataFilesTest extends OpenDataFilesTest<Schedule> {

    @Override
    protected CommonApp<Schedule> createCommonApp() {
        return new ProjectJobSchedulingApp();
    }
}
