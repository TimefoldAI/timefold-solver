package ai.timefold.solver.examples.projectjobscheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "ai.timefold.solver.examples.turtle", matches = "projectjobscheduling")
class ProjectJobSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<Schedule> {

    @Override
    protected CommonApp<Schedule> createCommonApp() {
        return new ProjectJobSchedulingApp();
    }

}
