package ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypestepscore;

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
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class PickedMoveTypeStepScoreDiffSubSingleStatistic<Solution_>
        extends PureSubSingleStatistic<Solution_, PickedMoveTypeStepScoreDiffStatisticPoint, LineChart<Long, Double>> {

    private PickedMoveTypeStepScoreDiffSubSingleStatistic() {
        // For JAXB.
    }

    public PickedMoveTypeStepScoreDiffSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        super(subSingleBenchmarkResult, SingleStatisticType.PICKED_MOVE_TYPE_STEP_SCORE_DIFF);
    }

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag, Solver<Solution_> solver) {
        registry.addListener(SolverMetric.PICKED_MOVE_TYPE_STEP_SCORE_DIFF, (timeMillisSpent, stepScope) -> {
            if (stepScope instanceof LocalSearchStepScope) {
                String moveType = ((LocalSearchStepScope<Solution_>) stepScope).getStep().getSimpleMoveTypeDescription();
                registry.extractScoreFromMeters(SolverMetric.PICKED_MOVE_TYPE_STEP_SCORE_DIFF,
                        runTag.and(Tag.of("move.type", moveType)),
                        score -> pointList.add(new PickedMoveTypeStepScoreDiffStatisticPoint(
                                timeMillisSpent, moveType, score)));
            }
        });
    }

    @Override
    protected String getCsvHeader() {
        return PickedMoveTypeStepScoreDiffStatisticPoint.buildCsvLine("timeMillisSpent", "moveType", "stepScoreDiff");
    }

    @Override
    protected PickedMoveTypeStepScoreDiffStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new PickedMoveTypeStepScoreDiffStatisticPoint(Long.parseLong(csvLine.get(0)),
                csvLine.get(1), scoreDefinition.parseScore(csvLine.get(2)));
    }

    @Override
    protected List<LineChart<Long, Double>> generateCharts(BenchmarkReport benchmarkReport) {
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (PickedMoveTypeStepScoreDiffStatisticPoint point : getPointList()) {
            long timeMillisSpent = point.getTimeMillisSpent();
            String moveType = point.getMoveType();
            double[] levelValues = point.getStepScoreDiff().toLevelDoubles();
            for (int i = 0; i < levelValues.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                if (i >= builderList.size()) {
                    builderList.add(new LineChart.Builder<>());
                }
                LineChart.Builder<Long, Double> builder = builderList.get(i);
                builder.add(moveType, timeMillisSpent, levelValues[i]);
            }
        }
        List<LineChart<Long, Double>> chartList = new ArrayList<>(builderList.size());
        for (int scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            String scoreLevelLabel = subSingleBenchmarkResult.getSingleBenchmarkResult().getProblemBenchmarkResult()
                    .findScoreLevelLabel(scoreLevelIndex);
            LineChart.Builder<Long, Double> builder = builderList.get(scoreLevelIndex);
            LineChart<Long, Double> chart = builder.build(
                    "pickedMoveTypeStepScoreDiffSubSingleStatisticChart" + scoreLevelIndex,
                    subSingleBenchmarkResult.getName() + " picked move type step " + scoreLevelLabel + " diff statistic",
                    "Time spent", "Step " + scoreLevelLabel + " diff", true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }

}
