package ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalbestscore;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.PureSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Tags;

public class ConstraintMatchTotalBestScoreSubSingleStatistic<Solution_>
        extends PureSubSingleStatistic<Solution_, ConstraintMatchTotalBestScoreStatisticPoint, LineChart<Long, Double>> {

    private ConstraintMatchTotalBestScoreSubSingleStatistic() {
        // For JAXB.
    }

    public ConstraintMatchTotalBestScoreSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        super(subSingleBenchmarkResult, SingleStatisticType.CONSTRAINT_MATCH_TOTAL_BEST_SCORE);
    }

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag, Solver<Solution_> solver) {
        DefaultSolver<Solution_> defaultSolver = (DefaultSolver<Solution_>) solver;
        defaultSolver.getSolverScope().getScoreDirector().overwriteConstraintMatchEnabledPreference(true);
        registry.addListener(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE,
                timeMillisSpent -> registry.extractConstraintSummariesFromMeters(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE,
                        runTag, constraintSummary -> pointList.add(new ConstraintMatchTotalBestScoreStatisticPoint(
                                timeMillisSpent,
                                constraintSummary.getConstraintPackage(),
                                constraintSummary.getConstraintName(),
                                constraintSummary.getCount(),
                                constraintSummary.getScore()))));
    }

    @Override
    protected String getCsvHeader() {
        return ConstraintMatchTotalBestScoreStatisticPoint.buildCsvLine(
                "timeMillisSpent", "constraintPackage", "constraintName",
                "constraintMatchCount", "scoreTotal");
    }

    @Override
    protected ConstraintMatchTotalBestScoreStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new ConstraintMatchTotalBestScoreStatisticPoint(Long.parseLong(csvLine.get(0)),
                csvLine.get(1), csvLine.get(2),
                Integer.parseInt(csvLine.get(3)), scoreDefinition.parseScore(csvLine.get(4)));
    }

    @Override
    protected List<LineChart<Long, Double>> generateCharts(BenchmarkReport benchmarkReport) {
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (ConstraintMatchTotalBestScoreStatisticPoint point : getPointList()) {
            long timeMillisSpent = point.getTimeMillisSpent();
            double[] levelValues = point.getScoreTotal().toLevelDoubles();
            for (int i = 0; i < levelValues.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                if (i >= builderList.size()) {
                    builderList.add(new LineChart.Builder<>());
                }
                LineChart.Builder<Long, Double> builder = builderList.get(i);
                String seriesLabel = point.getConstraintName() + " weight";
                // Only add changes
                double lastValue = (builder.count(seriesLabel) == 0) ? 0.0 : builder.getLastValue(seriesLabel);
                if (levelValues[i] != lastValue) {
                    builder.add(seriesLabel, timeMillisSpent, levelValues[i]);
                }
            }
        }
        long timeMillisSpent = subSingleBenchmarkResult.getTimeMillisSpent();
        for (LineChart.Builder<Long, Double> builder : builderList) {
            for (String key : builder.keys()) {
                // Draw a horizontal line from the last new best step to how long the solver actually ran
                builder.add(key, timeMillisSpent, builder.getLastValue(key));
            }
        }
        List<LineChart<Long, Double>> chartList = new ArrayList<>(builderList.size());
        for (int scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            String scoreLevelLabel = subSingleBenchmarkResult.getSingleBenchmarkResult().getProblemBenchmarkResult()
                    .findScoreLevelLabel(scoreLevelIndex);
            LineChart.Builder<Long, Double> builder = builderList.get(scoreLevelIndex);
            LineChart<Long, Double> chart =
                    builder.build("constraintMatchTotalBestScoreSubSingleStatisticChart" + scoreLevelIndex,
                            subSingleBenchmarkResult.getName() + " constraint match total best " + scoreLevelLabel
                                    + " diff statistic",
                            "Time spent", "Constraint match total " + scoreLevelLabel, true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }

}
