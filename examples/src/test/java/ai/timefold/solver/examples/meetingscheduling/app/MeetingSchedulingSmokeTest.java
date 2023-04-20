package ai.timefold.solver.examples.meetingscheduling.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.meetingscheduling.domain.MeetingSchedule;

class MeetingSchedulingSmokeTest extends SolverSmokeTest<MeetingSchedule, HardMediumSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/meetingscheduling/unsolved/50meetings-160timegrains-5rooms.xlsx";

    @Override
    protected MeetingSchedulingApp createCommonApp() {
        return new MeetingSchedulingApp();
    }

    @Override
    protected Stream<TestData<HardMediumSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardMediumSoftScore.of(-29, -344, -9227),
                        HardMediumSoftScore.ofUninitialized(-20, -116, -143, -5094)));
    }
}
