package ai.timefold.solver.examples.flightcrewscheduling.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightCrewSolution;

class FlightCrewSchedulingSmokeTest extends SolverSmokeTest<FlightCrewSolution, HardSoftLongScore> {

    private static final String UNSOLVED_DATA_FILE = "data/flightcrewscheduling/unsolved/175flights-7days-Europe.xlsx";

    @Override
    protected FlightCrewSchedulingApp createCommonApp() {
        return new FlightCrewSchedulingApp();
    }

    @Override
    protected Stream<TestData<HardSoftLongScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardSoftLongScore.ofSoft(-129000000),
                        HardSoftLongScore.ofSoft(-129000000)));
    }
}
