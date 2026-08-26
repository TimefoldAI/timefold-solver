package ai.timefold.solver.benchmark.impl.result;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.ReportHelper;
import ai.timefold.solver.benchmark.impl.statistic.StatisticUtils;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.io.jaxb.GenericJaxbIO;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.EnvironmentModeResolver;

/**
 * Represents 1 {@link Solver} configuration benchmarked on multiple problem instances (data sets).
 */
public class SolverBenchmarkResult {
    @XmlTransient // Bi-directional relationship restored through BenchmarkResultIO
    private PlannerBenchmarkResult plannerBenchmarkResult;

    private String name = null;

    private Integer subSingleCount = null;

    @XmlElement(namespace = SolverConfig.XML_NAMESPACE)
    private SolverConfig solverConfig = null;
    @XmlTransient // Restored through BenchmarkResultIO
    private ScoreDefinition scoreDefinition = null;

    @XmlElement(name = "singleBenchmarkResult")
    private List<SingleBenchmarkResult> singleBenchmarkResultList = null;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    private Integer failureCount = null;
    private Integer uninitializedSolutionCount = null;
    private Integer infeasibleScoreCount = null;
    private Score totalScore = null;
    private Score averageScore = null;
    // Not a Score because
    // - the squaring would cause overflow for relatively small int and long scores.
    // - standard deviation should not be rounded to integer numbers
    private double[] standardDeviationDoubles = null;
    private Score totalWinningScoreDifference = null;
    private ScoreDifferencePercentage averageWorstScoreDifferencePercentage = null;
    // The average of the average is not just the overall average if the SingleBenchmarkResult's timeMillisSpent differ
    private Long averageScoreCalculationSpeed = null;
    private Long averageMoveEvaluationSpeed = null;
    private Long averageTimeMillisSpent = null;
    private Double averageWorstScoreCalculationSpeedDifferencePercentage = null;

    // Ranking starts from 0
    private Integer ranking = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    private SolverBenchmarkResult() {
        // Required by JAXB
    }

    public SolverBenchmarkResult(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    public PlannerBenchmarkResult getPlannerBenchmarkResult() {
        return plannerBenchmarkResult;
    }

    public void setPlannerBenchmarkResult(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    /**
     * @return never null, filename safe
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSubSingleCount() {
        return subSingleCount;
    }

    public void setSubSingleCount(Integer subSingleCount) {
        this.subSingleCount = subSingleCount;
    }

    public SolverConfig getSolverConfig() {
        return solverConfig;
    }

    public void setSolverConfig(SolverConfig solverConfig) {
        this.solverConfig = solverConfig;
    }

    public ScoreDefinition getScoreDefinition() {
        return scoreDefinition;
    }

    public void setScoreDefinition(ScoreDefinition scoreDefinition) {
        this.scoreDefinition = scoreDefinition;
    }

    public List<SingleBenchmarkResult> getSingleBenchmarkResultList() {
        return singleBenchmarkResultList;
    }

    public void setSingleBenchmarkResultList(List<SingleBenchmarkResult> singleBenchmarkResultList) {
        this.singleBenchmarkResultList = singleBenchmarkResultList;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public Integer getUninitializedSolutionCount() {
        return uninitializedSolutionCount;
    }

    public Integer getInfeasibleScoreCount() {
        return infeasibleScoreCount;
    }

    public Score getTotalScore() {
        return totalScore;
    }

    public Score getAverageScore() {
        return averageScore;
    }

    public Score getTotalWinningScoreDifference() {
        return totalWinningScoreDifference;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public ScoreDifferencePercentage getAverageWorstScoreDifferencePercentage() {
        return averageWorstScoreDifferencePercentage;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Long getAverageScoreCalculationSpeed() {
        return averageScoreCalculationSpeed;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Long getAverageMoveEvaluationSpeed() {
        return averageMoveEvaluationSpeed;
    }

    public Long getAverageTimeMillisSpent() {
        return averageTimeMillisSpent;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Double getAverageWorstScoreCalculationSpeedDifferencePercentage() {
        return averageWorstScoreCalculationSpeedDifferencePercentage;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getAnchorId() {
        return ReportHelper.escapeHtmlId(name);
    }

    public String getNameWithFavoriteSuffix() {
        if (isFavorite()) {
            return name + " (favorite)";
        }
        return name;
    }

    public int getSuccessCount() {
        return getSingleBenchmarkResultList().size() - getFailureCount();
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean hasAnySuccess() {
        return getSuccessCount() > 0;
    }

    public boolean hasAnyFailure() {
        return failureCount > 0;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean hasAnyUninitializedSolution() {
        return uninitializedSolutionCount > 0;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean hasAnyInfeasibleScore() {
        return infeasibleScoreCount > 0;
    }

    public boolean isFavorite() {
        return ranking != null && ranking.intValue() == 0;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Score getAverageWinningScoreDifference() {
        if (totalWinningScoreDifference == null) {
            return null;
        }
        return totalWinningScoreDifference.divide(getSuccessCount());
    }

    /**
     * @param problemBenchmarkResult never null
     * @return sometimes null
     */
    @SuppressWarnings("unused") // Used by FreeMarker.
    public SingleBenchmarkResult findSingleBenchmark(ProblemBenchmarkResult problemBenchmarkResult) {
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            if (problemBenchmarkResult.equals(singleBenchmarkResult.getProblemBenchmarkResult())) {
                return singleBenchmarkResult;
            }
        }
        return null;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getSolverConfigAsString() {
        GenericJaxbIO<SolverConfig> xmlIO = new GenericJaxbIO<>(SolverConfig.class);
        StringWriter stringWriter = new StringWriter();
        xmlIO.write(solverConfig, stringWriter);
        return stringWriter.toString();
    }

    /**
     * @return the environment mode the solver as a whole runs in;
     *         not necessarily the strictest one used, see {@link #getStrictestEnvironmentMode()}
     */
    public EnvironmentMode getEnvironmentMode() {
        return EnvironmentModeResolver.resolve(solverConfig);
    }

    /**
     * @return the strictest environment mode any part of this solver's run uses,
     *         which is what determines the performance this benchmark measured
     */
    public EnvironmentMode getStrictestEnvironmentMode() {
        return EnvironmentModeResolver.resolveStrictest(solverConfig);
    }

    /**
     * The environment mode as the report states it. A single enum name cannot describe a config whose phases
     * disagree, and the difference is exactly what explains an otherwise inexplicable performance gap,
     * so the phase-level overrides are spelled out.
     *
     * @return for example {@code "PHASE_ASSERT"} or {@code "PHASE_ASSERT (localSearch: FULL_ASSERT)"}
     */
    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getEnvironmentModeLabel() {
        var environmentMode = getEnvironmentMode();
        var overrideList = describePhaseEnvironmentModeOverrides(environmentMode);
        if (overrideList.isEmpty()) {
            return environmentMode.name();
        }
        return "%s (%s)".formatted(environmentMode.name(), String.join(", ", overrideList));
    }

    private List<String> describePhaseEnvironmentModeOverrides(EnvironmentMode environmentMode) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (phaseConfigList == null || phaseConfigList.isEmpty()) {
            return Collections.emptyList();
        }
        var phaseEnvironmentModeList = EnvironmentModeResolver.resolvePhases(solverConfig);
        var overrideList = new ArrayList<String>(phaseConfigList.size());
        for (var i = 0; i < phaseConfigList.size(); i++) {
            // A phase which does not override the mode resolves to the solver's own, so this only lists real overrides.
            var phaseEnvironmentMode = phaseEnvironmentModeList.get(i);
            if (phaseEnvironmentMode != environmentMode) {
                overrideList.add("%s: %s".formatted(describePhase(phaseConfigList, i), phaseEnvironmentMode.name()));
            }
        }
        return overrideList;
    }

    /**
     * @return the phase's XML element name, suffixed with its position among the phases sharing that name
     *         when the config has more than one of them
     */
    private static String describePhase(List<PhaseConfig> phaseConfigList, int phaseIndex) {
        var phaseName = describePhaseType(phaseConfigList.get(phaseIndex));
        var sameNameCount = 0;
        var sameNameIndex = 0;
        for (var i = 0; i < phaseConfigList.size(); i++) {
            if (describePhaseType(phaseConfigList.get(i)).equals(phaseName)) {
                sameNameCount++;
                if (i == phaseIndex) {
                    sameNameIndex = sameNameCount;
                }
            }
        }
        return sameNameCount == 1 ? phaseName : "%s #%d".formatted(phaseName, sameNameIndex);
    }

    private static String describePhaseType(PhaseConfig phaseConfig) {
        return switch (phaseConfig) {
            case ConstructionHeuristicPhaseConfig ignored -> ConstructionHeuristicPhaseConfig.XML_ELEMENT_NAME;
            case CustomPhaseConfig ignored -> CustomPhaseConfig.XML_ELEMENT_NAME;
            case ExhaustiveSearchPhaseConfig ignored -> ExhaustiveSearchPhaseConfig.XML_ELEMENT_NAME;
            case LocalSearchPhaseConfig ignored -> LocalSearchPhaseConfig.XML_ELEMENT_NAME;
            case PartitionedSearchPhaseConfig ignored -> PartitionedSearchPhaseConfig.XML_ELEMENT_NAME;
            // A phase type unknown to this module, such as one added by enterprise.
            default -> phaseConfig.getClass().getSimpleName();
        };
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getStandardDeviationString() {
        return StatisticUtils.getStandardDeviationString(standardDeviationDoubles);
    }

    // ************************************************************************
    // Accumulate methods
    // ************************************************************************

    /**
     * Does not call {@link SingleBenchmarkResult#accumulateResults(BenchmarkReport)},
     * because {@link PlannerBenchmarkResult#accumulateResults(BenchmarkReport)} does that already on
     * {@link PlannerBenchmarkResult#getUnifiedProblemBenchmarkResultList()}.
     *
     * @param benchmarkReport never null
     */
    public void accumulateResults(BenchmarkReport benchmarkReport) {
        determineTotalsAndAverages();
        standardDeviationDoubles = StatisticUtils.determineStandardDeviationDoubles(singleBenchmarkResultList, averageScore,
                getSuccessCount());
    }

    protected void determineTotalsAndAverages() {
        failureCount = 0;
        boolean firstNonFailure = true;
        totalScore = null;
        totalWinningScoreDifference = null;
        ScoreDifferencePercentage totalWorstScoreDifferencePercentage = null;
        long totalScoreCalculationSpeed = 0L;
        long totalMoveEvaluationSpeed = 0L;
        long totalTimeMillisSpent = 0L;
        double totalWorstScoreCalculationSpeedDifferencePercentage = 0.0;
        uninitializedSolutionCount = 0;
        infeasibleScoreCount = 0;
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            if (singleBenchmarkResult.hasAnyFailure()) {
                failureCount++;
            } else {
                if (!singleBenchmarkResult.isInitialized()) {
                    uninitializedSolutionCount++;
                } else if (!singleBenchmarkResult.isScoreFeasible()) {
                    infeasibleScoreCount++;
                }
                if (firstNonFailure) {
                    totalScore = singleBenchmarkResult.getAverageScore();
                    totalWinningScoreDifference = singleBenchmarkResult.getWinningScoreDifference();
                    totalWorstScoreDifferencePercentage = singleBenchmarkResult.getWorstScoreDifferencePercentage();
                    totalScoreCalculationSpeed = singleBenchmarkResult.getScoreCalculationSpeed();
                    totalMoveEvaluationSpeed = singleBenchmarkResult.getMoveEvaluationSpeed();
                    totalTimeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                    totalWorstScoreCalculationSpeedDifferencePercentage = singleBenchmarkResult
                            .getWorstScoreCalculationSpeedDifferencePercentage();
                    firstNonFailure = false;
                } else {
                    totalScore = totalScore.add(singleBenchmarkResult.getAverageScore());
                    totalWinningScoreDifference = totalWinningScoreDifference.add(
                            singleBenchmarkResult.getWinningScoreDifference());
                    totalWorstScoreDifferencePercentage = totalWorstScoreDifferencePercentage.add(
                            singleBenchmarkResult.getWorstScoreDifferencePercentage());
                    totalScoreCalculationSpeed += singleBenchmarkResult.getScoreCalculationSpeed();
                    totalMoveEvaluationSpeed += singleBenchmarkResult.getMoveEvaluationSpeed();
                    totalTimeMillisSpent += singleBenchmarkResult.getTimeMillisSpent();
                    totalWorstScoreCalculationSpeedDifferencePercentage += singleBenchmarkResult
                            .getWorstScoreCalculationSpeedDifferencePercentage();
                }
            }
        }
        if (!firstNonFailure) {
            int successCount = getSuccessCount();
            averageScore = totalScore.divide(successCount);
            averageWorstScoreDifferencePercentage = totalWorstScoreDifferencePercentage.divide(successCount);
            averageScoreCalculationSpeed = totalScoreCalculationSpeed / successCount;
            averageMoveEvaluationSpeed = totalMoveEvaluationSpeed / successCount;
            averageTimeMillisSpent = totalTimeMillisSpent / successCount;
            averageWorstScoreCalculationSpeedDifferencePercentage = totalWorstScoreCalculationSpeedDifferencePercentage
                    / successCount;
        }
    }

    // ************************************************************************
    // Merger methods
    // ************************************************************************

    protected static Map<SolverBenchmarkResult, SolverBenchmarkResult> createMergeMap(
            PlannerBenchmarkResult newPlannerBenchmarkResult, List<SingleBenchmarkResult> singleBenchmarkResultList) {
        // IdentityHashMap because different SolverBenchmarkResult instances are never merged
        Map<SolverBenchmarkResult, SolverBenchmarkResult> mergeMap = new IdentityHashMap<>();
        Map<String, Integer> nameCountMap = new HashMap<>();
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            SolverBenchmarkResult oldResult = singleBenchmarkResult.getSolverBenchmarkResult();
            if (!mergeMap.containsKey(oldResult)) {
                SolverBenchmarkResult newResult = new SolverBenchmarkResult(newPlannerBenchmarkResult);
                Integer nameCount = nameCountMap.get(oldResult.name);
                if (nameCount == null) {
                    nameCount = 1;
                } else {
                    nameCount++;
                }
                nameCountMap.put(oldResult.name, nameCount);
                newResult.subSingleCount = oldResult.subSingleCount;
                newResult.solverConfig = oldResult.solverConfig;
                newResult.scoreDefinition = oldResult.scoreDefinition;
                newResult.singleBenchmarkResultList = new ArrayList<>(
                        oldResult.singleBenchmarkResultList.size());
                mergeMap.put(oldResult, newResult);
                newPlannerBenchmarkResult.getSolverBenchmarkResultList().add(newResult);
            }
        }
        // Make name unique
        for (Map.Entry<SolverBenchmarkResult, SolverBenchmarkResult> entry : mergeMap.entrySet()) {
            SolverBenchmarkResult oldResult = entry.getKey();
            SolverBenchmarkResult newResult = entry.getValue();
            if (nameCountMap.get(oldResult.name) > 1) {
                newResult.name = oldResult.name + " (" + oldResult.getPlannerBenchmarkResult().getName() + ")";
            } else {
                newResult.name = oldResult.name;
            }
        }
        return mergeMap;
    }

    @Override
    public String toString() {
        return getName();
    }

}
