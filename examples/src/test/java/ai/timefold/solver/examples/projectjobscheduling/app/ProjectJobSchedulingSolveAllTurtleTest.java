package ai.timefold.solver.examples.projectjobscheduling.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "projectjobscheduling")
class ProjectJobSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<Schedule> {

    @Override
    protected CommonApp<Schedule> createCommonApp() {
        return new ProjectJobSchedulingApp();
    }

}
