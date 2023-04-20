
package ai.timefold.solver.examples.conferencescheduling.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.conferencescheduling.domain.ConferenceSolution;

class ConferenceSchedulingSmokeTest extends SolverSmokeTest<ConferenceSolution, HardMediumSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/conferencescheduling/unsolved/72talks-12timeslots-10rooms.xlsx";

    @Override
    protected ConferenceSchedulingApp createCommonApp() {
        return new ConferenceSchedulingApp();
    }

    @Override
    protected Stream<TestData<HardMediumSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardMediumSoftScore.ofSoft(-1025250),
                        HardMediumSoftScore.ofSoft(-1100400)));
    }
}
