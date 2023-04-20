package ai.timefold.solver.benchmark.impl.statistic.movecountperstep;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class MoveCountPerStepSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<MoveCountPerStepStatisticPoint, MoveCountPerStepSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, MoveCountPerStepSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return MoveCountPerStepSubSingleStatistic::new;
    }

    @Override
    protected List<MoveCountPerStepStatisticPoint> getInputPoints() {
        return Collections.singletonList(new MoveCountPerStepStatisticPoint(Long.MAX_VALUE, 0, Long.MAX_VALUE));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<MoveCountPerStepStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1)
                .first()
                .matches(s -> s.getAcceptedMoveCount() == 0,
                        "Accepted move counts do not match.")
                .matches(s -> s.getSelectedMoveCount() == Long.MAX_VALUE,
                        "Selected move counts do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
