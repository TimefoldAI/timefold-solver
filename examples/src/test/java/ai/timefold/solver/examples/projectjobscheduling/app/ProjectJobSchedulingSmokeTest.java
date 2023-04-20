package ai.timefold.solver.examples.projectjobscheduling.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;

class ProjectJobSchedulingSmokeTest extends SolverSmokeTest<Schedule, HardMediumSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/projectjobscheduling/unsolved/A-4.json";

    @Override
    protected ProjectJobSchedulingApp createCommonApp() {
        return new ProjectJobSchedulingApp();
    }

    @Override
    protected Stream<TestData<HardMediumSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardMediumSoftScore.of(0, -181, -78),
                        HardMediumSoftScore.of(0, -211, -99)));
    }
}
