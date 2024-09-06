
package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.benchmark.impl.statistic.common.LongStatisticPoint;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class MoveCalculationSpeedSubSingleStatisticTest
        extends AbstractSubSingleStatisticTest<LongStatisticPoint, MoveCalculationSpeedSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, MoveCalculationSpeedSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return MoveCalculationSpeedSubSingleStatistic::new;
    }

    @Override
    protected List<LongStatisticPoint> getInputPoints() {
        return Collections.singletonList(new LongStatisticPoint(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<LongStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1)
                .first()
                .matches(s -> s.getValue() == Long.MAX_VALUE, "Move calculation speeds do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
