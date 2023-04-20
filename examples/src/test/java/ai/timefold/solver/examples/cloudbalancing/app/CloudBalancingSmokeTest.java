package ai.timefold.solver.examples.cloudbalancing.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;

class CloudBalancingSmokeTest extends SolverSmokeTest<CloudBalance, HardSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/cloudbalancing/unsolved/200computers-600processes.json";

    @Override
    protected CloudBalancingApp createCommonApp() {
        return new CloudBalancingApp();
    }

    @Override
    protected Stream<TestData<HardSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardSoftScore.ofSoft(-212900),
                        HardSoftScore.ofSoft(-237340)));
    }
}
