package ai.timefold.solver.examples.meetingscheduling.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.meetingscheduling.app.MeetingSchedulingApp;
import ai.timefold.solver.examples.meetingscheduling.domain.MeetingSchedule;

class MeetingSchedulingOpenDataFilesTest extends OpenDataFilesTest<MeetingSchedule> {

    @Override
    protected CommonApp<MeetingSchedule> createCommonApp() {
        return new MeetingSchedulingApp();
    }
}
