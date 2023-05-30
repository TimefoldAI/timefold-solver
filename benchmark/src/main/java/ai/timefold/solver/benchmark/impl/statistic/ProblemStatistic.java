package ai.timefold.solver.benchmark.impl.statistic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.Chart;
import ai.timefold.solver.benchmark.impl.report.ReportHelper;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.bestscore.BestScoreProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.bestsolutionmutation.BestSolutionMutationProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.memoryuse.MemoryUseProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.movecountperstep.MoveCountPerStepProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed.ScoreCalculationSpeedProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.stepscore.StepScoreProblemStatistic;

/**
 * 1 statistic of {@link ProblemBenchmarkResult}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({
        BestScoreProblemStatistic.class,
        StepScoreProblemStatistic.class,
        ScoreCalculationSpeedProblemStatistic.class,
        BestSolutionMutationProblemStatistic.class,
        MoveCountPerStepProblemStatistic.class,
        MemoryUseProblemStatistic.class
})
public abstract class ProblemStatistic<Chart_ extends Chart> implements ChartProvider<Chart_> {

    @XmlTransient // Bi-directional relationship restored through BenchmarkResultIO
    protected ProblemBenchmarkResult<Object> problemBenchmarkResult;

    @XmlTransient
    protected List<Chart_> chartList;

    protected ProblemStatisticType problemStatisticType;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    protected ProblemStatistic() {
        // For JAXB.
    }

    protected ProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult, ProblemStatisticType problemStatisticType) {
        this.problemBenchmarkResult = problemBenchmarkResult;
        this.problemStatisticType = problemStatisticType;
    }

    public ProblemBenchmarkResult getProblemBenchmarkResult() {
        return problemBenchmarkResult;
    }

    public void setProblemBenchmarkResult(ProblemBenchmarkResult problemBenchmarkResult) {
        this.problemBenchmarkResult = problemBenchmarkResult;
    }

    public ProblemStatisticType getProblemStatisticType() {
        return problemStatisticType;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getAnchorId() {
        return ReportHelper.escapeHtmlId(problemBenchmarkResult.getName() + "_" + problemStatisticType.name());
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<SubSingleStatistic> getSubSingleStatisticList() {
        List<SingleBenchmarkResult> singleBenchmarkResultList = problemBenchmarkResult.getSingleBenchmarkResultList();
        List<SubSingleStatistic> subSingleStatisticList = new ArrayList<>(singleBenchmarkResultList.size());
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            if (singleBenchmarkResult.getSubSingleBenchmarkResultList().isEmpty()) {
                continue;
            }
            // All subSingles have the same sub single statistics
            subSingleStatisticList.add(singleBenchmarkResult.getSubSingleBenchmarkResultList().get(0)
                    .getEffectiveSubSingleStatisticMap().get(problemStatisticType));
        }
        return subSingleStatisticList;
    }

    public abstract SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult);

    // ************************************************************************
    // Write methods
    // ************************************************************************

    @Override
    public final void createChartList(BenchmarkReport benchmarkReport) {
        this.chartList = generateCharts(benchmarkReport);
    }

    protected abstract List<Chart_> generateCharts(BenchmarkReport benchmarkReport);

    @Override
    public final List<Chart_> getChartList() {
        return chartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<String> getWarningList() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return problemBenchmarkResult + "_" + problemStatisticType;
    }

}
