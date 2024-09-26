package ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.common.LongStatisticPoint;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

final class ScoreCalculationSpeedSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<LongStatisticPoint, ScoreCalculationSpeedSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, ScoreCalculationSpeedSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return ScoreCalculationSpeedSubSingleStatistic::new;
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
                .matches(s -> s.getValue() == Long.MAX_VALUE, "Score calculation speeds do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

    @Test
    void generateCharts() {
        var problemBenchmarkResult = mock(ProblemBenchmarkResult.class);
        var benchmarkReport = mock(BenchmarkReport.class);
        var singleBenchmarkResult = mock(SingleBenchmarkResult.class);
        var solverBenchmarkResult = mock(SolverBenchmarkResult.class);
        var singleStatistic = mock(SubSingleStatistic.class);
        doReturn("Problem_0").when(problemBenchmarkResult).getName();
        doReturn(List.of(singleBenchmarkResult)).when(problemBenchmarkResult).getSingleBenchmarkResultList();
        doReturn(solverBenchmarkResult).when(singleBenchmarkResult).getSolverBenchmarkResult();
        doReturn("label").when(solverBenchmarkResult).getNameWithFavoriteSuffix();
        doReturn(true).when(singleBenchmarkResult).hasAllSuccess();
        doReturn(singleStatistic).when(singleBenchmarkResult).getSubSingleStatistic(any(ProblemStatisticType.class));
        doReturn(List.of(new LongStatisticPoint(Long.MIN_VALUE, Long.MIN_VALUE),
                new LongStatisticPoint(Long.MAX_VALUE, Long.MAX_VALUE))).when(singleStatistic).getPointList();
        var statistic = new ScoreCalculationSpeedProblemStatistic(problemBenchmarkResult);
        statistic.createChartList(benchmarkReport);
        assertThat(statistic.getChartList()).hasSize(1);
        var lineChart = statistic.getChartList().get(0);
        assertThat(lineChart.title()).isEqualTo("Problem_0 score calculation speed statistic");
        assertThat(lineChart.xLabel()).isEqualTo("Time spent");
        assertThat(lineChart.yLabel()).isEqualTo("Score calculation speed per second");
    }
}
