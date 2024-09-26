
package ai.timefold.solver.benchmark.impl.statistic.movecountpertype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

final class MoveCountPerTypeSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<MoveCountPerTypeStatisticPoint, MoveCountPerTypeSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, MoveCountPerTypeSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return MoveCountPerTypeSubSingleStatistic::new;
    }

    @Override
    protected List<MoveCountPerTypeStatisticPoint> getInputPoints() {
        return List.of(new MoveCountPerTypeStatisticPoint("Move Type 1", Long.MIN_VALUE),
                new MoveCountPerTypeStatisticPoint("Move Type 2", Long.MAX_VALUE));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<MoveCountPerTypeStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(2)
                .anyMatch(m -> m.getCount() == Long.MIN_VALUE)
                .anyMatch(m -> m.getCount() == Long.MAX_VALUE);
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
        doReturn(List.of(new MoveCountPerTypeStatisticPoint("Move Type 1", Long.MIN_VALUE),
                new MoveCountPerTypeStatisticPoint("Move Type 2", Long.MAX_VALUE))).when(singleStatistic).getPointList();
        var statistic = new MoveCountPerTypeProblemStatistic(problemBenchmarkResult);
        statistic.createChartList(benchmarkReport);
        assertThat(statistic.getChartList()).hasSize(1);
        var barChart = statistic.getChartList().get(0);
        assertThat(barChart.title()).isEqualTo("Problem_0 move count per type statistic");
        assertThat(barChart.xLabel()).isEqualTo("Type");
        assertThat(barChart.yLabel()).isEqualTo("Count");
    }
}
