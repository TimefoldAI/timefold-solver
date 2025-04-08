package ai.timefold.solver.benchmark.impl.result;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.ranking.ScoreSubSingleBenchmarkRankingComparator;
import ai.timefold.solver.benchmark.impl.ranking.SubSingleBenchmarkRankBasedComparator;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.ReportHelper;
import ai.timefold.solver.benchmark.impl.statistic.StatisticUtils;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.util.ConfigUtils;

/**
 * Represents 1 benchmark for 1 {@link Solver} configuration for 1 problem instance (data set).
 */
public class SingleBenchmarkResult implements BenchmarkResult {

    // Required by JAXB to refer to existing instances of this class
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1L);

    @XmlID
    @XmlAttribute
    private String id = String.valueOf(ID_GENERATOR.getAndIncrement());

    @XmlTransient // Bi-directional relationship restored through BenchmarkResultIO
    private SolverBenchmarkResult solverBenchmarkResult;
    @XmlTransient // Bi-directional relationship restored through BenchmarkResultIO
    private ProblemBenchmarkResult problemBenchmarkResult;

    @XmlElement(name = "subSingleBenchmarkResult")
    private List<SubSingleBenchmarkResult> subSingleBenchmarkResultList = null;

    private Long usedMemoryAfterInputSolution = null;

    private Integer failureCount = null;
    private Score totalScore = null;
    private Score averageScore = null;
    private boolean allScoresInitialized = false;
    private SubSingleBenchmarkResult median = null;
    private SubSingleBenchmarkResult best = null;
    private SubSingleBenchmarkResult worst = null;
    // Not a Score because
    // - the squaring would cause overflow for relatively small int and long scores.
    // - standard deviation should not be rounded to integer numbers
    private double[] standardDeviationDoubles = null;
    private long timeMillisSpent = -1L;
    private long scoreCalculationCount = -1L;
    private long moveEvaluationCount = -1L;
    private String scoreExplanationSummary = null;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    // Compared to winningSingleBenchmarkResult in the same ProblemBenchmarkResult (which might not be the overall favorite)
    private Score<?> winningScoreDifference = null;
    private ScoreDifferencePercentage worstScoreDifferencePercentage = null;
    private Double worstScoreCalculationSpeedDifferencePercentage = null;

    // Ranking starts from 0
    private Integer ranking = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public SingleBenchmarkResult() {
        // Required by JAXB
    }

    public SingleBenchmarkResult(SolverBenchmarkResult solverBenchmarkResult, ProblemBenchmarkResult problemBenchmarkResult) {
        this.solverBenchmarkResult = solverBenchmarkResult;
        this.problemBenchmarkResult = problemBenchmarkResult;
    }

    public void initSubSingleStatisticMaps() {
        for (var subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.initSubSingleStatisticMap();
        }
    }

    public SolverBenchmarkResult getSolverBenchmarkResult() {
        return solverBenchmarkResult;
    }

    public void setSolverBenchmarkResult(SolverBenchmarkResult solverBenchmarkResult) {
        this.solverBenchmarkResult = solverBenchmarkResult;
    }

    public ProblemBenchmarkResult getProblemBenchmarkResult() {
        return problemBenchmarkResult;
    }

    public void setProblemBenchmarkResult(ProblemBenchmarkResult problemBenchmarkResult) {
        this.problemBenchmarkResult = problemBenchmarkResult;
    }

    public List<SubSingleBenchmarkResult> getSubSingleBenchmarkResultList() {
        return subSingleBenchmarkResultList;
    }

    public void setSubSingleBenchmarkResultList(List<SubSingleBenchmarkResult> subSingleBenchmarkResultList) {
        this.subSingleBenchmarkResultList = subSingleBenchmarkResultList;
    }

    /**
     * @return null if {@link PlannerBenchmarkResult#hasMultipleParallelBenchmarks()} return true
     */
    public Long getUsedMemoryAfterInputSolution() {
        return usedMemoryAfterInputSolution;
    }

    public void setUsedMemoryAfterInputSolution(Long usedMemoryAfterInputSolution) {
        this.usedMemoryAfterInputSolution = usedMemoryAfterInputSolution;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public void setTimeMillisSpent(long timeMillisSpent) {
        this.timeMillisSpent = timeMillisSpent;
    }

    public long getScoreCalculationCount() {
        return scoreCalculationCount;
    }

    public void setScoreCalculationCount(long scoreCalculationCount) {
        this.scoreCalculationCount = scoreCalculationCount;
    }

    public long getMoveEvaluationCount() {
        return moveEvaluationCount;
    }

    public void setMoveEvaluationCount(long moveEvaluationCount) {
        this.moveEvaluationCount = moveEvaluationCount;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getScoreExplanationSummary() {
        return scoreExplanationSummary;
    }

    public Score<?> getWinningScoreDifference() {
        return winningScoreDifference;
    }

    public void setWinningScoreDifference(Score<?> winningScoreDifference) {
        this.winningScoreDifference = winningScoreDifference;
    }

    public ScoreDifferencePercentage getWorstScoreDifferencePercentage() {
        return worstScoreDifferencePercentage;
    }

    public void setWorstScoreDifferencePercentage(ScoreDifferencePercentage worstScoreDifferencePercentage) {
        this.worstScoreDifferencePercentage = worstScoreDifferencePercentage;
    }

    public Double getWorstScoreCalculationSpeedDifferencePercentage() {
        return worstScoreCalculationSpeedDifferencePercentage;
    }

    public void setWorstScoreCalculationSpeedDifferencePercentage(Double worstScoreCalculationSpeedDifferencePercentage) {
        this.worstScoreCalculationSpeedDifferencePercentage = worstScoreCalculationSpeedDifferencePercentage;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    @Override
    public Score getAverageScore() {
        return averageScore;
    }

    public void setAverageAndTotalScoreForTesting(Score<?> averageAndTotalScore, boolean allScoresInitialized) {
        this.averageScore = averageAndTotalScore;
        this.totalScore = averageAndTotalScore;
        this.allScoresInitialized = allScoresInitialized;
    }

    public SubSingleBenchmarkResult getMedian() {
        return median;
    }

    public SubSingleBenchmarkResult getBest() {
        return best;
    }

    public SubSingleBenchmarkResult getWorst() {
        return worst;
    }

    public Score<?> getTotalScore() {
        return totalScore;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getAnchorId() {
        return ReportHelper.escapeHtmlId(getName());
    }

    /**
     * @return never null, filename safe
     */
    @Override
    public String getName() {
        return problemBenchmarkResult.getName() + "_" + solverBenchmarkResult.getName();
    }

    @Override
    public boolean hasAllSuccess() {
        return failureCount != null && failureCount == 0;
    }

    public boolean isInitialized() {
        return averageScore != null && allScoresInitialized;
    }

    @Override
    public boolean hasAnyFailure() {
        return failureCount != null && failureCount != 0;
    }

    public boolean isScoreFeasible() {
        return averageScore.isFeasible();
    }

    public Long getScoreCalculationSpeed() {
        var scoreCalculationCountByThousand = scoreCalculationCount * 1000L;
        // Avoid divide by zero exception on a fast CPU
        return timeMillisSpent == 0L ? scoreCalculationCountByThousand : scoreCalculationCountByThousand / timeMillisSpent;
    }

    public Long getMoveEvaluationSpeed() {
        var moveEvaluationCountByThousand = moveEvaluationCount * 1000L;
        // Avoid divide by zero exception on a fast CPU
        return timeMillisSpent == 0L ? moveEvaluationCountByThousand : moveEvaluationCountByThousand / timeMillisSpent;
    }

    @SuppressWarnings("unused") // Used By FreeMarker.
    public boolean isWinner() {
        return ranking != null && ranking.intValue() == 0;
    }

    public SubSingleStatistic getSubSingleStatistic(ProblemStatisticType problemStatisticType) {
        return getMedian().getEffectiveSubSingleStatisticMap().get(problemStatisticType);
    }

    public int getSuccessCount() {
        return subSingleBenchmarkResultList.size() - failureCount;
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public String getStandardDeviationString() {
        return StatisticUtils.getStandardDeviationString(standardDeviationDoubles);
    }

    // ************************************************************************
    // Accumulate methods
    // ************************************************************************

    @Override
    public String getResultDirectoryName() {
        return solverBenchmarkResult.getName();
    }

    @Override
    public File getResultDirectory() {
        return new File(problemBenchmarkResult.getProblemReportDirectory(), getResultDirectoryName());
    }

    public void makeDirs() {
        var singleReportDirectory = getResultDirectory();
        singleReportDirectory.mkdirs();
        for (var subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.makeDirs();
        }
    }

    public int getSubSingleCount() {
        return subSingleBenchmarkResultList.size();
    }

    public void accumulateResults(BenchmarkReport benchmarkReport) {
        for (var subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.accumulateResults(benchmarkReport);
        }
        determineTotalsAndAveragesAndRanking();
        standardDeviationDoubles = StatisticUtils.determineStandardDeviationDoubles(subSingleBenchmarkResultList, averageScore,
                getSuccessCount());
        determineRepresentativeSubSingleBenchmarkResult();
    }

    private void determineRepresentativeSubSingleBenchmarkResult() {
        if (subSingleBenchmarkResultList == null || subSingleBenchmarkResultList.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot get representative subSingleBenchmarkResult from empty subSingleBenchmarkResultList.");
        }
        var subSingleBenchmarkResultListCopy = new ArrayList<>(subSingleBenchmarkResultList);
        // sort (according to ranking) so that the best subSingle is at index 0
        subSingleBenchmarkResultListCopy.sort(new SubSingleBenchmarkRankBasedComparator());
        best = subSingleBenchmarkResultListCopy.get(0);
        worst = subSingleBenchmarkResultListCopy.get(subSingleBenchmarkResultListCopy.size() - 1);
        median = subSingleBenchmarkResultListCopy.get(ConfigUtils.ceilDivide(subSingleBenchmarkResultListCopy.size() - 1, 2));
        usedMemoryAfterInputSolution = median.getUsedMemoryAfterInputSolution();
        timeMillisSpent = median.getTimeMillisSpent();
        scoreCalculationCount = median.getScoreCalculationCount();
        moveEvaluationCount = median.getMoveEvaluationCount();
        scoreExplanationSummary = median.getScoreExplanationSummary();
    }

    private void determineTotalsAndAveragesAndRanking() {
        failureCount = 0;
        var firstNonFailure = true;
        totalScore = null;
        var successResultList = new ArrayList<>(subSingleBenchmarkResultList);
        // Do not rank a SubSingleBenchmarkResult that has a failure
        for (var it = successResultList.iterator(); it.hasNext();) {
            var subSingleBenchmarkResult = it.next();
            if (subSingleBenchmarkResult.hasAnyFailure()) {
                failureCount++;
                it.remove();
            } else {
                var isInitialized = subSingleBenchmarkResult.isInitialized();
                if (firstNonFailure) {
                    totalScore = subSingleBenchmarkResult.getAverageScore();
                    allScoresInitialized = isInitialized;
                    firstNonFailure = false;
                } else {
                    totalScore = totalScore.add(subSingleBenchmarkResult.getAverageScore());
                    allScoresInitialized = allScoresInitialized || isInitialized;
                }
            }
        }
        if (!firstNonFailure) {
            averageScore = totalScore.divide(getSuccessCount());
        }
        determineRanking(successResultList);
    }

    private static void determineRanking(List<SubSingleBenchmarkResult> rankedSubSingleBenchmarkResultList) {
        var subSingleBenchmarkRankingComparator = new ScoreSubSingleBenchmarkRankingComparator();
        rankedSubSingleBenchmarkResultList.sort(Collections.reverseOrder(subSingleBenchmarkRankingComparator));
        var ranking = 0;
        SubSingleBenchmarkResult previousSubSingleBenchmarkResult = null;
        var previousSameRankingCount = 0;
        for (var subSingleBenchmarkResult : rankedSubSingleBenchmarkResultList) {
            if (previousSubSingleBenchmarkResult != null
                    && subSingleBenchmarkRankingComparator.compare(previousSubSingleBenchmarkResult,
                            subSingleBenchmarkResult) != 0) {
                ranking += previousSameRankingCount;
                previousSameRankingCount = 0;
            }
            subSingleBenchmarkResult.setRanking(ranking);
            previousSubSingleBenchmarkResult = subSingleBenchmarkResult;
            previousSameRankingCount++;
        }
    }

    // ************************************************************************
    // Merger methods
    // ************************************************************************

    protected static SingleBenchmarkResult createMerge(
            SolverBenchmarkResult solverBenchmarkResult, ProblemBenchmarkResult problemBenchmarkResult,
            SingleBenchmarkResult oldResult) {
        var newResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        newResult.subSingleBenchmarkResultList = new ArrayList<>(oldResult.getSubSingleBenchmarkResultList().size());
        var subSingleBenchmarkIndex = 0;
        for (var oldSubResult : oldResult.subSingleBenchmarkResultList) {
            SubSingleBenchmarkResult.createMerge(newResult, oldSubResult, subSingleBenchmarkIndex);
            subSingleBenchmarkIndex++;
        }
        newResult.median = oldResult.median;
        newResult.best = oldResult.best;
        newResult.worst = oldResult.worst;
        solverBenchmarkResult.getSingleBenchmarkResultList().add(newResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(newResult);
        return newResult;
    }

    @Override
    public String toString() {
        return getName();
    }

}
