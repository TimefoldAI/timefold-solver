package ai.timefold.solver.service.worker.impl;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverRunResult;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.service.definition.api.SolverModel;

import org.jspecify.annotations.Nullable;

/**
 * This class aims to only copy the current state of a SolverJob without replicating any behavior.
 * It is intended to be used only for publishing state changes via events.
 */
public class SolverWorkerJobState implements SolverJob<SolverModel> {

    public static SolverWorkerJobState ofRunning(SolverStatus solverStatus, SolverJob<SolverModel> job) {
        // Avoid calling the job to get stats since that can recursively call problem finder in the SolverWorker
        var problemSizeStatistics = solverStatus == SolverStatus.SOLVING_ACTIVE ? job.getProblemSizeStatistics() : null;
        return new SolverWorkerJobState(
                (String) job.getProblemId(),
                solverStatus,
                problemSizeStatistics,
                job.getSolvingDuration(),
                job.getScoreCalculationCount(),
                job.getScoreCalculationSpeed(),
                job.isTerminatedEarly(),
                job.getMoveEvaluationCount(),
                job.getMoveEvaluationSpeed(),
                null,
                null,
                null,
                null);
    }

    public static SolverWorkerJobState ofTerminated(SolverStatus solverStatus, SolverJob<SolverModel> job,
            SolverRunSummary runSummary) {
        return new SolverWorkerJobState(
                (String) job.getProblemId(),
                solverStatus,
                runSummary.problemSizeStatistics(),
                runSummary.solvingDuration(),
                runSummary.scoreCalculationCount(),
                runSummary.scoreCalculationSpeed(),
                runSummary.terminatedEarly(),
                runSummary.moveEvaluationCount(),
                runSummary.moveEvaluationSpeed(),
                runSummary.solutionInitialized(),
                runSummary.termination(),
                runSummary.lastPhase(),
                runSummary.timeToBestSolution());
    }

    private final String problemId;
    private final SolverStatus solverStatus;
    private final ProblemSizeStatistics problemSizeStatistics;
    private final Duration solvingDuration;
    private final long scoreCalculationCount;
    private final long scoreCalculationSpeed;
    private final boolean isTerminatedEarly;
    private final long moveEvaluationCount;
    private final long moveEvaluationSpeed;

    // the following fields are only available from the SolverRunSummary of a finished run
    private final @Nullable Boolean solutionInitialized;
    private final @Nullable TerminationId termination;
    private final @Nullable PhaseId lastPhase;
    private final @Nullable Duration timeToBestSolution;

    public SolverWorkerJobState(
            String problemId,
            SolverStatus solverStatus,
            ProblemSizeStatistics problemSizeStatistics,
            Duration solvingDuration,
            long scoreCalculationCount,
            long scoreCalculationSpeed,
            boolean isTerminatedEarly,
            long moveEvaluationCount,
            long moveEvaluationSpeed,
            @Nullable Boolean solutionInitialized,
            @Nullable TerminationId termination,
            @Nullable PhaseId lastPhase,
            @Nullable Duration timeToBestSolution) {
        this.problemId = problemId;
        this.problemSizeStatistics = problemSizeStatistics;
        this.solvingDuration = solvingDuration;
        this.scoreCalculationCount = scoreCalculationCount;
        this.scoreCalculationSpeed = scoreCalculationSpeed;
        this.solverStatus = solverStatus;
        this.isTerminatedEarly = isTerminatedEarly;
        this.moveEvaluationCount = moveEvaluationCount;
        this.moveEvaluationSpeed = moveEvaluationSpeed;
        this.solutionInitialized = solutionInitialized;
        this.termination = termination;
        this.lastPhase = lastPhase;
        this.timeToBestSolution = timeToBestSolution;
    }

    public SolverWorkerJobState(SolverStatus solverStatus, SolverJob<SolverModel> job) {
        this.problemId = (String) job.getProblemId();
        this.solverStatus = solverStatus;
        this.solvingDuration = job.getSolvingDuration();
        this.scoreCalculationCount = job.getScoreCalculationCount();
        this.scoreCalculationSpeed = job.getScoreCalculationSpeed();
        this.isTerminatedEarly = job.isTerminatedEarly();
        // Avoid calling the job to get stats since that can recursively call problem finder in the SolverWorker
        this.problemSizeStatistics = solverStatus == SolverStatus.SOLVING_ACTIVE ? job.getProblemSizeStatistics() : null;
        this.moveEvaluationCount = job.getMoveEvaluationCount();
        this.moveEvaluationSpeed = job.getMoveEvaluationSpeed();
        this.solutionInitialized = null;
        this.termination = null;
        this.lastPhase = null;
        this.timeToBestSolution = null;
    }

    @Override
    public String getProblemId() {
        return this.problemId;
    }

    @Override
    public SolverStatus getSolverStatus() {
        return this.solverStatus;
    }

    @Override
    public CompletableFuture<Void> addProblemChange(ProblemChange<SolverModel> problemChange) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> addProblemChanges(List<ProblemChange<SolverModel>> list) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void terminateEarly() {
        //no-op
    }

    @Override
    public boolean isTerminatedEarly() {
        return isTerminatedEarly;
    }

    @Override
    public SolverModel getFinalBestSolution() {
        return null;
    }

    @Override
    public SolverRunResult<SolverModel> getResult() {
        return null;
    }

    @Override
    public Duration getSolvingDuration() {
        return this.solvingDuration;
    }

    @Override
    public long getScoreCalculationCount() {
        return this.scoreCalculationCount;
    }

    @Override
    public long getMoveEvaluationCount() {
        return this.moveEvaluationCount;
    }

    @Override
    public ProblemSizeStatistics getProblemSizeStatistics() {
        return this.problemSizeStatistics;
    }

    @Override
    public long getScoreCalculationSpeed() {
        return this.scoreCalculationSpeed;
    }

    @Override
    public long getMoveEvaluationSpeed() {
        return this.moveEvaluationSpeed;
    }

    /**
     * @return null until the run finished; see {@link SolverRunSummary#solutionInitialized()}
     */
    public @Nullable Boolean isSolutionInitialized() {
        return this.solutionInitialized;
    }

    /**
     * @return null until the run finished, or if the run finished without a termination;
     *         see {@link SolverRunSummary#termination()}
     */
    public @Nullable TerminationId getTermination() {
        return this.termination;
    }

    /**
     * @return null until the run finished, or if no phase ran; see {@link SolverRunSummary#lastPhase()}
     */
    public @Nullable PhaseId getLastPhase() {
        return this.lastPhase;
    }

    /**
     * @return null until the run finished; see {@link SolverRunSummary#timeToBestSolution()}
     */
    public @Nullable Duration getTimeToBestSolution() {
        return this.timeToBestSolution;
    }

    @Override
    public String toString() {
        return "SolverWorkerJobState{" +
                "problemId='" + problemId + '\'' +
                ", problemSizeStatistics=" + problemSizeStatistics +
                ", solvingDuration=" + solvingDuration +
                ", scoreCalculationCount=" + scoreCalculationCount +
                ", scoreCalculationSpeed=" + scoreCalculationSpeed +
                ", solverStatus=" + solverStatus +
                ", isTerminatedEarly=" + isTerminatedEarly +
                ", moveEvaluationCount=" + moveEvaluationCount +
                ", moveEvaluationSpeed=" + moveEvaluationSpeed +
                ", solutionInitialized=" + solutionInitialized +
                ", termination=" + termination +
                ", lastPhase=" + lastPhase +
                ", timeToBestSolution=" + timeToBestSolution +
                '}';
    }
}
