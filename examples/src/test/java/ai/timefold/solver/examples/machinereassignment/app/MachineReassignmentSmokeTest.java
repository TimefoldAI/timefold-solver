package ai.timefold.solver.examples.machinereassignment.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;

class MachineReassignmentSmokeTest extends SolverSmokeTest<MachineReassignment, HardSoftLongScore> {

    private static final String UNSOLVED_DATA_FILE = "data/machinereassignment/unsolved/model_a2_1.json";

    @Override
    protected MachineReassignmentApp createCommonApp() {
        return new MachineReassignmentApp();
    }

    @Override
    protected Stream<TestData<HardSoftLongScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardSoftLongScore.ofSoft(-39203859),
                        HardSoftLongScore.ofSoft(-61212262)));
    }
}
