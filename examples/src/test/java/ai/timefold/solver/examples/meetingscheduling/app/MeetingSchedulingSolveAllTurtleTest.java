package ai.timefold.solver.examples.meetingscheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.meetingscheduling.domain.MeetingSchedule;

class MeetingSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<MeetingSchedule> {

    @Override
    protected CommonApp<MeetingSchedule> createCommonApp() {
        return new MeetingSchedulingApp();
    }
}
