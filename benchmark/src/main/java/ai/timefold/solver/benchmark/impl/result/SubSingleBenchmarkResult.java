package ai.timefold.solver.benchmark.impl.result;

import static ai.timefold.solver.core.impl.util.MathUtils.getSpeed;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.PureSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticType;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalbestscore.ConstraintMatchTotalBestScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalstepscore.ConstraintMatchTotalStepScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypebestscore.PickedMoveTypeBestScoreDiffSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypestepscore.PickedMoveTypeStepScoreDiffSubSingleStatistic;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents 1 benchmark run for 1 Single Benchmark configuration for 1 {@link Solver} configuration for 1 problem
 * instance (data set).
 */
public class SubSingleBenchmarkResult implements BenchmarkResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubSingleBenchmarkResult.class);

    @XmlTransient // Bi-directional relationship restored through BenchmarkResultIO
    private SingleBenchmarkResult singleBenchmarkResult;
    private int subSingleBenchmarkIndex;

    @XmlElements({
            @XmlElement(name = "constraintMatchTotalBestScoreSubSingleStatistic",
                    type = ConstraintMatchTotalBestScoreSubSingleStatistic.class),
            @XmlElement(name = "constraintMatchTotalStepScoreSubSingleStatistic",
                    type = ConstraintMatchTotalStepScoreSubSingleStatistic.class),
            @XmlElement(name = "pickedMoveTypeBestScoreDiffSubSingleStatistic",
                    type = PickedMoveTypeBestScoreDiffSubSingleStatistic.class),
            @XmlElement(name = "pickedMoveTypeStepScoreDiffSubSingleStatistic",
                    type = PickedMoveTypeStepScoreDiffSubSingleStatistic.class)
    })
    private List<PureSubSingleStatistic> pureSubSingleStatisticList = null;

    @XmlTransient // Lazily restored when read through ProblemStatistic and CSV files
    private Map<StatisticType, SubSingleStatistic> effectiveSubSingleStatisticMap;

    private Long usedMemoryAfterInputSolution = null;

    private Boolean succeeded = null;
    private Score<?> score = null;
    private long timeMillisSpent = -1L;
    private long scoreCalculationCount = -1L;
    private long moveEvaluationCount = -1L;
    private String scoreExplanationSummary = null;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    // Ranking starts from 0
    private Integer ranking = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    private SubSingleBenchmarkResult() {
        // Required by JAXB
    }

    public SubSingleBenchmarkResult(SingleBenchmarkResult singleBenchmarkResult, int subSingleBenchmarkIndex) {
        this.singleBenchmarkResult = singleBenchmarkResult;
        this.subSingleBenchmarkIndex = subSingleBenchmarkIndex;
    }

    public List<PureSubSingleStatistic> getPureSubSingleStatisticList() {
        return pureSubSingleStatisticList;
    }

    public void setPureSubSingleStatisticList(List<PureSubSingleStatistic> pureSubSingleStatisticList) {
        this.pureSubSingleStatisticList = pureSubSingleStatisticList;
    }

    public void initSubSingleStatisticMap() {
        List<ProblemStatistic> problemStatisticList = singleBenchmarkResult.getProblemBenchmarkResult()
                .getProblemStatisticList();
        effectiveSubSingleStatisticMap = new HashMap<>(
                problemStatisticList.size() + pureSubSingleStatisticList.size());
        for (ProblemStatistic problemStatistic : problemStatisticList) {
            SubSingleStatistic subSingleStatistic = problemStatistic.createSubSingleStatistic(this);
            effectiveSubSingleStatisticMap.put(subSingleStatistic.getStatisticType(), subSingleStatistic);
        }
        for (PureSubSingleStatistic pureSubSingleStatistic : pureSubSingleStatisticList) {
            effectiveSubSingleStatisticMap.put(pureSubSingleStatistic.getStatisticType(), pureSubSingleStatistic);
        }
    }

    public SingleBenchmarkResult getSingleBenchmarkResult() {
        return singleBenchmarkResult;
    }

    public void setSingleBenchmarkResult(SingleBenchmarkResult singleBenchmarkResult) {
        this.singleBenchmarkResult = singleBenchmarkResult;
    }

    public int getSubSingleBenchmarkIndex() {
        return subSingleBenchmarkIndex;
    }

    public Map<StatisticType, SubSingleStatistic> getEffectiveSubSingleStatisticMap() {
        return effectiveSubSingleStatisticMap;
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

    public Boolean getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(Boolean succeeded) {
        this.succeeded = succeeded;
    }

    public Score<?> getScore() {
        return score;
    }

    public void setScore(Score<?> score) {
        this.score = score;
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

    public String getScoreExplanationSummary() {
        return scoreExplanationSummary;
    }

    public void setScoreExplanationSummary(String scoreExplanationSummary) {
        this.scoreExplanationSummary = scoreExplanationSummary;
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

    /**
     * @return never null, filename safe
     */
    @Override
    public String getName() {
        return singleBenchmarkResult.getName() + "_" + subSingleBenchmarkIndex;
    }

    @Override
    public boolean hasAllSuccess() {
        return succeeded != null && succeeded.booleanValue();
    }

    public boolean isInitialized() {
        return score != null && score.isSolutionInitialized();
    }

    @Override
    public boolean hasAnyFailure() {
        return succeeded != null && !succeeded.booleanValue();
    }

    public boolean isScoreFeasible() {
        return score.isFeasible();
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Long getScoreCalculationSpeed() {
        return getSpeed(scoreCalculationCount, this.timeMillisSpent);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public Long getMoveEvaluationSpeed() {
        return getSpeed(moveEvaluationCount, this.timeMillisSpent);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean isWinner() {
        return ranking != null && ranking.intValue() == 0;
    }

    public SubSingleStatistic getSubSingleStatistic(StatisticType statisticType) {
        return effectiveSubSingleStatisticMap.get(statisticType);
    }

    @Override
    public Score<?> getAverageScore() {
        return getScore();
    }

    // ************************************************************************
    // Accumulate methods
    // ************************************************************************

    @Override
    public String getResultDirectoryName() {
        return "sub" + subSingleBenchmarkIndex;
    }

    @Override
    public File getResultDirectory() {
        return new File(singleBenchmarkResult.getResultDirectory(), getResultDirectoryName());
    }

    public void makeDirs() {
        File subSingleReportDirectory = getResultDirectory();
        subSingleReportDirectory.mkdirs();
    }

    public void accumulateResults(BenchmarkReport benchmarkReport) {

    }

    // ************************************************************************
    // Merger methods
    // ************************************************************************

    protected static SubSingleBenchmarkResult createMerge(
            SingleBenchmarkResult singleBenchmarkResult, SubSingleBenchmarkResult oldResult,
            int subSingleBenchmarkIndex) {
        SubSingleBenchmarkResult newResult = new SubSingleBenchmarkResult(singleBenchmarkResult, subSingleBenchmarkIndex);
        newResult.pureSubSingleStatisticList = new ArrayList<>(oldResult.pureSubSingleStatisticList.size());
        for (PureSubSingleStatistic oldSubSingleStatistic : oldResult.pureSubSingleStatisticList) {
            newResult.pureSubSingleStatisticList.add(
                    oldSubSingleStatistic.getStatisticType().buildPureSubSingleStatistic(newResult));
        }

        newResult.initSubSingleStatisticMap();
        for (SubSingleStatistic newSubSingleStatistic : newResult.effectiveSubSingleStatisticMap.values()) {
            SubSingleStatistic oldSubSingleStatistic = oldResult
                    .getSubSingleStatistic(newSubSingleStatistic.getStatisticType());
            if (!oldSubSingleStatistic.getCsvFile().exists()) {
                if (oldResult.hasAnyFailure()) {
                    newSubSingleStatistic.initPointList();
                    LOGGER.debug("Old result ({}) is a failure, skipping merge of its sub single statistic ({}).",
                            oldResult, oldSubSingleStatistic);
                    continue;
                } else {
                    throw new IllegalStateException("Could not find old result's (" + oldResult
                            + ") sub single statistic's (" + oldSubSingleStatistic + ") CSV file.");
                }
            }
            oldSubSingleStatistic.unhibernatePointList();
            newSubSingleStatistic.setPointList(oldSubSingleStatistic.getPointList());
            oldSubSingleStatistic.hibernatePointList();
        }
        // Skip oldResult.reportDirectory
        // Skip oldResult.usedMemoryAfterInputSolution
        newResult.succeeded = oldResult.succeeded;
        newResult.score = oldResult.score;
        newResult.timeMillisSpent = oldResult.timeMillisSpent;
        newResult.scoreCalculationCount = oldResult.scoreCalculationCount;
        newResult.moveEvaluationCount = oldResult.moveEvaluationCount;

        singleBenchmarkResult.getSubSingleBenchmarkResultList().add(newResult);
        return newResult;
    }

    @Override
    public String toString() {
        return getName();
    }

}
