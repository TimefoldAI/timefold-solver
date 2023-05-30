package ai.timefold.solver.benchmark.impl.report;

import static java.lang.Double.isFinite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.benchmark.impl.ranking.SolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.result.LoggingLevel;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.PureSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class BenchmarkReport {

    public static Configuration createFreeMarkerConfiguration() {
        Configuration freeMarkerCfg = new Configuration(new Version(2, 3, 32));
        freeMarkerCfg.setDefaultEncoding("UTF-8");
        return freeMarkerCfg;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReport.class);

    public static final int CHARTED_SCORE_LEVEL_SIZE = 15;
    public static final int LOG_SCALE_MIN_DATASETS_COUNT = 5;

    private final PlannerBenchmarkResult plannerBenchmarkResult;

    private Locale locale = null;
    private ZoneId timezoneId = null;
    private Comparator<SolverBenchmarkResult> solverRankingComparator = null;
    private SolverRankingWeightFactory solverRankingWeightFactory = null;
    private List<BarChart<Double>> bestScoreSummaryChartList = null;
    private List<LineChart<Long, Double>> bestScoreScalabilitySummaryChartList = null;
    private List<BoxPlot> bestScoreDistributionSummaryChartList = null;
    private List<BarChart<Double>> winningScoreDifferenceSummaryChartList = null;
    private List<BarChart<Double>> worstScoreDifferencePercentageSummaryChartList = null;
    private LineChart<Long, Long> scoreCalculationSpeedSummaryChart;
    private BarChart<Double> worstScoreCalculationSpeedDifferencePercentageSummaryChart = null;
    private BarChart<Long> timeSpentSummaryChart = null;
    private LineChart<Long, Long> timeSpentScalabilitySummaryChart = null;
    private List<LineChart<Long, Double>> bestScorePerTimeSpentSummaryChartList = null;

    private Integer defaultShownScoreLevelIndex = null;
    private File htmlOverviewFile = null;

    public BenchmarkReport(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    public PlannerBenchmarkResult getPlannerBenchmarkResult() {
        return plannerBenchmarkResult;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public ZoneId getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(ZoneId timezoneId) {
        this.timezoneId = timezoneId;
    }

    public Comparator<SolverBenchmarkResult> getSolverRankingComparator() {
        return solverRankingComparator;
    }

    public void setSolverRankingComparator(Comparator<SolverBenchmarkResult> solverRankingComparator) {
        this.solverRankingComparator = solverRankingComparator;
    }

    public SolverRankingWeightFactory getSolverRankingWeightFactory() {
        return solverRankingWeightFactory;
    }

    public void setSolverRankingWeightFactory(SolverRankingWeightFactory solverRankingWeightFactory) {
        this.solverRankingWeightFactory = solverRankingWeightFactory;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<BarChart<Double>> getBestScoreSummaryChartList() {
        return bestScoreSummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<LineChart<Long, Double>> getBestScoreScalabilitySummaryChartList() {
        return bestScoreScalabilitySummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<BoxPlot> getBestScoreDistributionSummaryChartList() {
        return bestScoreDistributionSummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<BarChart<Double>> getWinningScoreDifferenceSummaryChartList() {
        return winningScoreDifferenceSummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<BarChart<Double>> getWorstScoreDifferencePercentageSummaryChartList() {
        return worstScoreDifferencePercentageSummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public LineChart<Long, Long> getScoreCalculationSpeedSummaryChart() {
        return scoreCalculationSpeedSummaryChart;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BarChart<Double> getWorstScoreCalculationSpeedDifferencePercentageSummaryChart() {
        return worstScoreCalculationSpeedDifferencePercentageSummaryChart;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BarChart<Long> getTimeSpentSummaryChart() {
        return timeSpentSummaryChart;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public LineChart<Long, Long> getTimeSpentScalabilitySummaryChart() {
        return timeSpentScalabilitySummaryChart;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<LineChart<Long, Double>> getBestScorePerTimeSpentSummaryChartList() {
        return bestScorePerTimeSpentSummaryChartList;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Integer getDefaultShownScoreLevelIndex() {
        return defaultShownScoreLevelIndex;
    }

    public File getHtmlOverviewFile() {
        return htmlOverviewFile;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getSolverRankingClassSimpleName() {
        Class solverRankingClass = getSolverRankingClass();
        return solverRankingClass == null ? null : solverRankingClass.getSimpleName();
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getSolverRankingClassFullName() {
        Class solverRankingClass = getSolverRankingClass();
        return solverRankingClass == null ? null : solverRankingClass.getName();
    }

    // ************************************************************************
    // Write methods
    // ************************************************************************

    public void writeReport() {
        LOGGER.info("Generating benchmark report...");
        plannerBenchmarkResult.accumulateResults(this);
        bestScoreSummaryChartList = createBestScoreSummaryChart();
        bestScoreScalabilitySummaryChartList = createBestScoreScalabilitySummaryChart();
        winningScoreDifferenceSummaryChartList = createWinningScoreDifferenceSummaryChart();
        worstScoreDifferencePercentageSummaryChartList = createWorstScoreDifferencePercentageSummaryChart();
        bestScoreDistributionSummaryChartList = createBestScoreDistributionSummaryChart();
        scoreCalculationSpeedSummaryChart = createScoreCalculationSpeedSummaryChart();
        worstScoreCalculationSpeedDifferencePercentageSummaryChart =
                createWorstScoreCalculationSpeedDifferencePercentageSummaryChart();
        timeSpentSummaryChart = createTimeSpentSummaryChart();
        timeSpentScalabilitySummaryChart = createTimeSpentScalabilitySummaryChart();
        bestScorePerTimeSpentSummaryChartList = createBestScorePerTimeSpentSummaryChart();

        for (ProblemBenchmarkResult<?> problemBenchmarkResult : plannerBenchmarkResult.getUnifiedProblemBenchmarkResultList()) {
            for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult
                        .getSubSingleBenchmarkResultList()) {
                    if (!subSingleBenchmarkResult.hasAllSuccess()) {
                        continue;
                    }
                    for (SubSingleStatistic<?, ?> subSingleStatistic : subSingleBenchmarkResult
                            .getEffectiveSubSingleStatisticMap().values()) {
                        try {
                            subSingleStatistic.unhibernatePointList();
                        } catch (IllegalStateException e) {
                            if (!plannerBenchmarkResult.getAggregation()) {
                                throw new IllegalStateException("Failed to unhibernate point list of SubSingleStatistic ("
                                        + subSingleStatistic + ") of SubSingleBenchmark (" + subSingleBenchmarkResult + ").",
                                        e);
                            }
                            LOGGER.trace("This is expected, aggregator doesn't copy CSV files. Could not read CSV file "
                                    + "({}) of sub single statistic ({}).", subSingleStatistic.getCsvFile().getAbsolutePath(),
                                    subSingleStatistic);
                        }
                    }
                }
            }
        }

        List<Chart> chartsToWrite = new ArrayList<>(bestScoreSummaryChartList);
        chartsToWrite.addAll(bestScoreSummaryChartList);
        chartsToWrite.addAll(bestScoreScalabilitySummaryChartList);
        chartsToWrite.addAll(winningScoreDifferenceSummaryChartList);
        chartsToWrite.addAll(worstScoreDifferencePercentageSummaryChartList);
        chartsToWrite.addAll(bestScoreDistributionSummaryChartList);
        chartsToWrite.add(scoreCalculationSpeedSummaryChart);
        chartsToWrite.add(worstScoreCalculationSpeedDifferencePercentageSummaryChart);
        chartsToWrite.add(timeSpentSummaryChart);
        chartsToWrite.add(timeSpentScalabilitySummaryChart);
        chartsToWrite.addAll(bestScorePerTimeSpentSummaryChartList);

        for (ProblemBenchmarkResult<?> problemBenchmarkResult : plannerBenchmarkResult
                .getUnifiedProblemBenchmarkResultList()) {
            if (problemBenchmarkResult.hasAnySuccess()) {
                for (ProblemStatistic<?> problemStatistic : problemBenchmarkResult.getProblemStatisticList()) {
                    problemStatistic.createChartList(this);
                    chartsToWrite.addAll(problemStatistic.getChartList());
                }
                for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                    if (singleBenchmarkResult.hasAllSuccess()) {
                        for (PureSubSingleStatistic<?, ?, ?> pureSubSingleStatistic : singleBenchmarkResult.getMedian()
                                .getPureSubSingleStatisticList()) {
                            pureSubSingleStatistic.createChartList(this);
                            chartsToWrite.addAll(pureSubSingleStatistic.getChartList());
                        }
                    }
                }
            }
        }

        // Now write all JavaScript files for the charts.
        chartsToWrite.parallelStream()
                .forEach(c -> c
                        .writeToFile(plannerBenchmarkResult.getBenchmarkReportDirectory().toPath().resolve("website/js")));

        for (ProblemBenchmarkResult<?> problemBenchmarkResult : plannerBenchmarkResult.getUnifiedProblemBenchmarkResultList()) {
            for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult
                        .getSubSingleBenchmarkResultList()) {
                    if (!subSingleBenchmarkResult.hasAllSuccess()) {
                        continue;
                    }
                    for (SubSingleStatistic<?, ?> subSingleStatistic : subSingleBenchmarkResult
                            .getEffectiveSubSingleStatisticMap().values()) {
                        if (plannerBenchmarkResult.getAggregation()) {
                            subSingleStatistic.setPointList(null);
                        } else {
                            subSingleStatistic.hibernatePointList();
                        }
                    }
                }
            }
        }
        determineDefaultShownScoreLevelIndex();
        writeHtmlOverviewFile();
    }

    public List<String> getWarningList() {
        List<String> warningList = new ArrayList<>();
        String javaVmName = System.getProperty("java.vm.name");
        if (javaVmName != null && javaVmName.contains("Client VM")) {
            warningList.add("The Java VM (" + javaVmName + ") is the Client VM."
                    + " This decreases performance."
                    + " Maybe start the java process with the argument \"-server\" to get better results.");
        }
        Integer parallelBenchmarkCount = plannerBenchmarkResult.getParallelBenchmarkCount();
        Integer availableProcessors = plannerBenchmarkResult.getAvailableProcessors();
        if (parallelBenchmarkCount != null && availableProcessors != null
                && parallelBenchmarkCount > availableProcessors) {
            warningList.add("The parallelBenchmarkCount (" + parallelBenchmarkCount
                    + ") is higher than the number of availableProcessors (" + availableProcessors + ")."
                    + " This decreases performance."
                    + " Maybe reduce the parallelBenchmarkCount.");
        }
        EnvironmentMode environmentMode = plannerBenchmarkResult.getEnvironmentMode();
        if (environmentMode != null && environmentMode.isAsserted()) {
            warningList.add("The environmentMode (" + environmentMode + ") is asserting."
                    + " This decreases performance."
                    + " Maybe set the environmentMode to " + EnvironmentMode.REPRODUCIBLE + ".");
        }
        LoggingLevel loggingLevelTimefoldCore = plannerBenchmarkResult.getLoggingLevelTimefoldSolverCore();
        if (loggingLevelTimefoldCore == LoggingLevel.TRACE) {
            warningList.add("The loggingLevel (" + loggingLevelTimefoldCore + ") of ai.timefold.solver.core is high."
                    + " This decreases performance."
                    + " Maybe set the loggingLevel to " + LoggingLevel.DEBUG + " or lower.");
        }
        return warningList;
    }

    private List<BarChart<Double>> createBestScoreSummaryChart() {
        List<BarChart.Builder<Double>> builderList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String problemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.hasAllSuccess()) {
                    double[] levelValues = singleBenchmarkResult.getAverageScore().toLevelDoubles();
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new BarChart.Builder<>());
                        }
                        if (isFinite(levelValues[i])) {
                            BarChart.Builder<Double> builder = builderList.get(i);
                            builder.add(solverLabel, problemLabel, levelValues[i]);
                            if (solverBenchmarkResult.isFavorite()) {
                                builder.markFavorite(solverLabel);
                            }
                        }
                    }
                }
            }
        }
        List<BarChart<Double>> chartList = new ArrayList<>(builderList.size());
        int scoreLevelIndex = 0;
        for (BarChart.Builder<Double> builder : builderList) {
            String scoreLevelLabel = plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            BarChart<Double> chart = builder.build("bestScoreSummaryChart" + scoreLevelIndex,
                    "Best " + scoreLevelLabel + " summary (higher is better)", "Data", "Best " + scoreLevelLabel, false);
            chartList.add(chart);
            scoreLevelIndex++;
        }
        return chartList;
    }

    private List<LineChart<Long, Double>> createBestScoreScalabilitySummaryChart() {
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.hasAllSuccess()) {
                    long problemScale = singleBenchmarkResult.getProblemBenchmarkResult().getProblemScale();
                    double[] levelValues = singleBenchmarkResult.getAverageScore().toLevelDoubles();
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new LineChart.Builder<>());
                        }
                        LineChart.Builder<Long, Double> builder = builderList.get(i);
                        builder.add(solverLabel, problemScale, levelValues[i]);
                        if (solverBenchmarkResult.isFavorite()) {
                            builder.markFavorite(solverLabel);
                        }
                    }
                }
            }
        }
        List<LineChart<Long, Double>> chartList = new ArrayList<>(builderList.size());
        int scoreLevelIndex = 0;
        for (LineChart.Builder<Long, Double> builder : builderList) {
            String scoreLevelLabel = plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            chartList.add(builder.build("bestScoreScalabilitySummaryChart" + scoreLevelIndex,
                    "Best " + scoreLevelLabel + " scalability summary (higher is better)", "Problem scale",
                    "Best " + scoreLevelLabel, false, false, false));
            scoreLevelIndex++;
        }
        return chartList;
    }

    private List<BoxPlot> createBestScoreDistributionSummaryChart() {
        List<BoxPlot.Builder> builderList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String problemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.hasAllSuccess()) {
                    List<List<Double>> distributionLevelList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
                    for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult
                            .getSubSingleBenchmarkResultList()) {
                        double[] levelValues = subSingleBenchmarkResult.getAverageScore().toLevelDoubles();
                        for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                            if (i >= distributionLevelList.size()) {
                                distributionLevelList.add(new ArrayList<>(singleBenchmarkResult.getSubSingleCount()));
                            }
                            distributionLevelList.get(i).add(levelValues[i]);
                        }
                    }
                    for (int i = 0; i < distributionLevelList.size() && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new BoxPlot.Builder());
                        }
                        BoxPlot.Builder builder = builderList.get(i);
                        for (double y : distributionLevelList.get(i)) {
                            builder.add(solverLabel, problemLabel, y);
                        }
                        if (solverBenchmarkResult.isFavorite()) {
                            builder.markFavorite(solverLabel);
                        }
                    }
                }
            }
        }
        List<BoxPlot> chartList = new ArrayList<>(builderList.size());
        int scoreLevelIndex = 0;
        for (BoxPlot.Builder builder : builderList) {
            String scoreLevelLabel = plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            BoxPlot boxPlot = builder.build("bestScoreDistributionSummaryChart" + scoreLevelIndex,
                    "Best " + scoreLevelLabel + " distribution summary (higher is better)", "Data", "Best " + scoreLevelLabel);
            chartList.add(boxPlot);
            scoreLevelIndex++;
        }
        return chartList;
    }

    private List<BarChart<Double>> createWinningScoreDifferenceSummaryChart() {
        return createScoreDifferenceSummaryChart(
                singleBenchmarkResult -> singleBenchmarkResult.getWinningScoreDifference().toLevelDoubles(),
                scoreLevelIndex -> "winningScoreDifferenceSummaryChart" + scoreLevelIndex,
                scoreLevelLabel -> "Winning " + scoreLevelLabel + " difference summary (higher is better)",
                scoreLevelLabel -> "Winning " + scoreLevelLabel + " difference");
    }

    private List<BarChart<Double>> createWorstScoreDifferencePercentageSummaryChart() {
        return createScoreDifferenceSummaryChart(
                singleBenchmarkResult -> singleBenchmarkResult.getWorstScoreDifferencePercentage().percentageLevels(),
                scoreLevelIndex -> "worstScoreDifferencePercentageSummaryChart" + scoreLevelIndex,
                scoreLevelLabel -> "Worst " + scoreLevelLabel + " difference percentage" + " summary (higher is better)",
                scoreLevelLabel -> "Worst " + scoreLevelLabel + " difference percentage");
    }

    private List<BarChart<Double>> createScoreDifferenceSummaryChart(
            Function<SingleBenchmarkResult, double[]> scoreLevelValueFunction, IntFunction<String> idFunction,
            Function<String, String> titleFunction, Function<String, String> yLabelFunction) {
        List<BarChart.Builder<Double>> builderList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String problemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.hasAllSuccess()) {
                    double[] levelValues = scoreLevelValueFunction.apply(singleBenchmarkResult);
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new BarChart.Builder<>());
                        }
                        if (isFinite(levelValues[i])) {
                            BarChart.Builder<Double> builder = builderList.get(i);
                            builder.add(solverLabel, problemLabel, levelValues[i] * 100);
                            if (solverBenchmarkResult.isFavorite()) {
                                builder.markFavorite(solverLabel);
                            }
                        }
                    }
                }
            }
        }
        List<BarChart<Double>> chartList = new ArrayList<>(builderList.size());
        int scoreLevelIndex = 0;
        for (BarChart.Builder<Double> builder : builderList) {
            String scoreLevelLabel = plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            BarChart<Double> chart = builder.build(idFunction.apply(scoreLevelIndex), titleFunction.apply(scoreLevelLabel),
                    "Data", yLabelFunction.apply(scoreLevelLabel), false);
            chartList.add(chart);
            scoreLevelIndex++;
        }
        return chartList;
    }

    private LineChart<Long, Long> createScoreCalculationSpeedSummaryChart() {
        return createScalabilitySummaryChart(SingleBenchmarkResult::getScoreCalculationSpeed,
                "scoreCalculationSpeedSummaryChart", "Score calculation speed summary (higher is better)",
                "Score calculation speed per second", false);
    }

    private BarChart<Double> createWorstScoreCalculationSpeedDifferencePercentageSummaryChart() {
        return createSummaryBarChart(result -> result.getWorstScoreCalculationSpeedDifferencePercentage() * 100,
                "worstScoreCalculationSpeedDifferencePercentageSummaryChart",
                "Worst score calculation speed difference percentage summary (higher is better)",
                "Worst score calculation speed difference percentage", false);
    }

    private BarChart<Long> createTimeSpentSummaryChart() {
        return createSummaryBarChart(SingleBenchmarkResult::getTimeMillisSpent, "timeSpentSummaryChart",
                "Time spent summary (lower time is better)", "Time spent", true);
    }

    private <N extends Number & Comparable<N>> BarChart<N> createSummaryBarChart(
            Function<SingleBenchmarkResult, N> valueFunction, String id, String title, String yLabel, boolean timeOnY) {
        BarChart.Builder<N> builder = new BarChart.Builder<>();
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String problemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.hasAllSuccess()) {
                    builder.add(solverLabel, problemLabel, valueFunction.apply(singleBenchmarkResult));
                    if (solverBenchmarkResult.isFavorite()) {
                        builder.markFavorite(solverLabel);
                    }
                }
            }
        }
        return builder.build(id, title, "Data", yLabel, timeOnY);
    }

    private LineChart<Long, Long> createTimeSpentScalabilitySummaryChart() {
        return createScalabilitySummaryChart(SingleBenchmarkResult::getTimeMillisSpent, "timeSpentScalabilitySummaryChart",
                "Time spent scalability summary (lower is better)", "Time spent", true);
    }

    private LineChart<Long, Long> createScalabilitySummaryChart(ToLongFunction<SingleBenchmarkResult> valueFunction, String id,
            String title, String yLabel, boolean timeOnY) {
        LineChart.Builder<Long, Long> builder = new LineChart.Builder<>();
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            if (solverBenchmarkResult.isFavorite()) {
                builder.markFavorite(solverLabel);
            }
            solverBenchmarkResult.getSingleBenchmarkResultList()
                    .stream()
                    .filter(SingleBenchmarkResult::hasAllSuccess)
                    .forEach(singleBenchmarkResult -> {
                        long problemScale = singleBenchmarkResult.getProblemBenchmarkResult().getProblemScale();
                        long timeMillisSpent = valueFunction.applyAsLong(singleBenchmarkResult);
                        builder.add(solverLabel, problemScale, timeMillisSpent);
                    });
        }
        return builder.build(id, title, "Problem scale", yLabel, false, false, timeOnY);
    }

    private List<LineChart<Long, Double>> createBestScorePerTimeSpentSummaryChart() {
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.hasAllSuccess()) {
                    long timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                    double[] levelValues = singleBenchmarkResult.getAverageScore().toLevelDoubles();
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new LineChart.Builder<>());
                        }
                        LineChart.Builder<Long, Double> builder = builderList.get(i);
                        builder.add(solverLabel, timeMillisSpent, levelValues[i]);
                        if (solverBenchmarkResult.isFavorite()) {
                            builder.markFavorite(solverLabel);
                        }
                    }
                }
            }
        }
        bestScorePerTimeSpentSummaryChartList = new ArrayList<>(builderList.size());
        int scoreLevelIndex = 0;
        for (LineChart.Builder<Long, Double> builder : builderList) {
            String scoreLevelLabel = plannerBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            LineChart<Long, Double> chart = builder.build("bestScorePerTimeSpentSummaryChart" + scoreLevelIndex,
                    "Best " + scoreLevelLabel + " per time spent summary (higher left is better)", "Time spent",
                    "Best " + scoreLevelLabel, false, true, false);
            bestScorePerTimeSpentSummaryChartList.add(chart);
            scoreLevelIndex++;
        }
        return bestScorePerTimeSpentSummaryChartList;
    }

    // ************************************************************************
    // Chart helper methods
    // ************************************************************************

    private void determineDefaultShownScoreLevelIndex() {
        defaultShownScoreLevelIndex = Integer.MAX_VALUE;
        for (ProblemBenchmarkResult<Object> problemBenchmarkResult : plannerBenchmarkResult
                .getUnifiedProblemBenchmarkResultList()) {
            if (problemBenchmarkResult.hasAnySuccess()) {
                double[] winningScoreLevels =
                        problemBenchmarkResult.getWinningSingleBenchmarkResult().getAverageScore().toLevelDoubles();
                int[] differenceCount = new int[winningScoreLevels.length];
                for (int i = 0; i < differenceCount.length; i++) {
                    differenceCount[i] = 0;
                }
                for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                    if (singleBenchmarkResult.hasAllSuccess()) {
                        double[] scoreLevels = singleBenchmarkResult.getAverageScore().toLevelDoubles();
                        for (int i = 0; i < scoreLevels.length; i++) {
                            if (scoreLevels[i] != winningScoreLevels[i]) {
                                differenceCount[i] = differenceCount[i] + 1;
                            }
                        }
                    }
                }
                int firstInterestingLevel = differenceCount.length - 1;
                for (int i = 0; i < differenceCount.length; i++) {
                    if (differenceCount[i] > 0) {
                        firstInterestingLevel = i;
                        break;
                    }
                }
                if (defaultShownScoreLevelIndex > firstInterestingLevel) {
                    defaultShownScoreLevelIndex = firstInterestingLevel;
                }
            }
        }
    }

    private void writeHtmlOverviewFile() {
        File benchmarkReportDirectory = plannerBenchmarkResult.getBenchmarkReportDirectory();
        WebsiteResourceUtils.copyResourcesTo(benchmarkReportDirectory);

        htmlOverviewFile = new File(benchmarkReportDirectory, "index.html");
        Configuration freemarkerCfg = createFreeMarkerConfiguration();
        freemarkerCfg.setLocale(locale);
        freemarkerCfg.setClassForTemplateLoading(BenchmarkReport.class, "");
        freemarkerCfg.setCustomNumberFormats(Map.of("msDuration", MillisecondDurationNumberFormatFactory.INSTANCE));

        String templateFilename = "benchmarkReport.html.ftl";
        Map<String, Object> model = new HashMap<>();
        model.put("benchmarkReport", this);
        model.put("reportHelper", new ReportHelper());

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(htmlOverviewFile), "UTF-8")) {
            Template template = freemarkerCfg.getTemplate(templateFilename);
            template.process(model, writer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not read templateFilename (" + templateFilename
                    + ") or write htmlOverviewFile (" + htmlOverviewFile + ").", e);
        } catch (TemplateException e) {
            throw new IllegalArgumentException("Can not process Freemarker templateFilename (" + templateFilename
                    + ") to htmlOverviewFile (" + htmlOverviewFile + ").", e);
        }
    }

    private Class getSolverRankingClass() {
        if (solverRankingComparator != null) {
            return solverRankingComparator.getClass();
        } else if (solverRankingWeightFactory != null) {
            return solverRankingWeightFactory.getClass();
        } else {
            return null;
        }
    }

}
