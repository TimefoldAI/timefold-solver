package ai.timefold.solver.benchmark.impl.statistic.common;

import static java.util.Collections.singletonList;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;

public abstract class AbstractTimeLineChartProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private String reportFileName;
    private String reportTitle;
    private String yLabel;
    private ProblemStatisticType statisticType;

    protected AbstractTimeLineChartProblemStatistic(ProblemStatisticType statisticType) {
        super(null, statisticType);
        this.statisticType = statisticType;
    }

    protected AbstractTimeLineChartProblemStatistic(ProblemStatisticType statisticType,
            ProblemBenchmarkResult<?> problemBenchmarkResult, String reportFileName, String reportTitle, String yLabel) {
        super(problemBenchmarkResult, statisticType);
        this.statisticType = statisticType;
        this.reportFileName = reportFileName;
        this.reportTitle = reportTitle;
        this.yLabel = yLabel;
    }

    /**
     * @return never null
     */
    @Override
    protected List<LineChart<Long, Long>> generateCharts(BenchmarkReport benchmarkReport) {
        var builder = new LineChart.Builder<Long, Long>();
        for (var singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            var solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<LongStatisticPoint> points = subSingleStatistic.getPointList();
                for (var point : points) {
                    var timeMillisSpent = point.getTimeMillisSpent();
                    var calculationSpeed = point.getValue();
                    builder.add(solverLabel, timeMillisSpent, calculationSpeed);
                }
            }
            if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                builder.markFavorite(solverLabel);
            }
        }
        return singletonList(builder.build(reportFileName, reportTitle, "Time spent", yLabel, false, true, false));
    }

    public void setReportFileName(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void setyLabel(String yLabel) {
        this.yLabel = yLabel;
    }

}
