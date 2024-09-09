
package ai.timefold.solver.benchmark.impl.statistic.moveevaluationspeed;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.benchmark.impl.statistic.common.LongStatisticPoint;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class MoveEvaluationSpeedSubSingleStatisticTest
        extends AbstractSubSingleStatisticTest<LongStatisticPoint, MoveEvaluationSpeedSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, MoveEvaluationSpeedSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return MoveEvaluationSpeedSubSingleStatistic::new;
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
                .matches(s -> s.getValue() == Long.MAX_VALUE, "Move evaluation speeds do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
