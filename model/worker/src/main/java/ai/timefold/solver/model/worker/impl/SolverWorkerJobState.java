package ai.timefold.solver.model.worker.impl;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.model.definition.api.SolverModel;

/**
 * This class aims to only copy the current state of a SolverJob without replicating any behaviour.
 * It is intended to be used only for publishing state changes via events.
 */
public class SolverWorkerJobState implements SolverJob<SolverModel> {

    private final String problemId;
    private final ProblemSizeStatistics problemSizeStatistics;
    private final Duration solvingDuration;
    private final long scoreCalculationCount;
    private final long scoreCalculationSpeed;
    private final SolverStatus solverStatus;
    private final boolean isTerminatedEarly;
    private final long moveEvaluationCount;
    private final long moveEvaluationSpeed;

    // solver status must be provided from outside to not rely on solver job itself as this can lead to recurring load of data sets which is an infinite loop
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
    public SolverModel getFinalBestSolution() throws InterruptedException, ExecutionException {
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
                '}';
    }
}
