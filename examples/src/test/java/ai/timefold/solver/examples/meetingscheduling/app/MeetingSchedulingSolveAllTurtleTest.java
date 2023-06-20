package ai.timefold.solver.examples.meetingscheduling.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.meetingscheduling.domain.MeetingSchedule;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "meetingscheduling")
class MeetingSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<MeetingSchedule> {

    @Override
    protected CommonApp<MeetingSchedule> createCommonApp() {
        return new MeetingSchedulingApp();
    }
}
