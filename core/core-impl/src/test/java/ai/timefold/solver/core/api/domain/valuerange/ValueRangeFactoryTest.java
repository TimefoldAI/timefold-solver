package ai.timefold.solver.core.api.domain.valuerange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.TestdataValueRangeEntity;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.TestdataValueRangeSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class ValueRangeFactoryTest {

    @Test
    void solve() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataValueRangeSolution.class, TestdataValueRangeEntity.class);

        TestdataValueRangeSolution solution = new TestdataValueRangeSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataValueRangeEntity("e1"), new TestdataValueRangeEntity("e2")));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
    }

}
