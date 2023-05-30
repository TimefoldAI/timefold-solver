package ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypebestscore;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class PickedMoveTypeBestScoreSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<PickedMoveTypeBestScoreDiffStatisticPoint, PickedMoveTypeBestScoreDiffSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, PickedMoveTypeBestScoreDiffSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return PickedMoveTypeBestScoreDiffSubSingleStatistic::new;
    }

    @Override
    protected List<PickedMoveTypeBestScoreDiffStatisticPoint> getInputPoints() {
        return Collections
                .singletonList(new PickedMoveTypeBestScoreDiffStatisticPoint(Long.MAX_VALUE, "SwapMove(A.class, B.class)",
                        SimpleScore.of(Integer.MAX_VALUE)));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<PickedMoveTypeBestScoreDiffStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1);
        PickedMoveTypeBestScoreDiffStatisticPoint point = outputPoints.get(0);
        assertions.assertThat(point.getMoveType())
                .isEqualTo("SwapMove(A.class, B.class)");
        assertions.assertThat(point.getBestScoreDiff())
                .isEqualTo(SimpleScore.of(Integer.MAX_VALUE));
        assertions.assertThat(point.getTimeMillisSpent())
                .isEqualTo(Long.MAX_VALUE);
    }

}
