package ai.timefold.solver.service.worker.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.LinkedHashMap;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;
import ai.timefold.solver.core.impl.phase.DefaultPhaseId;
import ai.timefold.solver.core.impl.solver.DefaultSolverRunSummary;
import ai.timefold.solver.core.impl.solver.termination.DefaultTerminationId;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class SolverRunSummaryMessageTest {

    private static final TerminationId SOLVER_ROOT = new DefaultTerminationId(TerminationType.OR, null, 0, null);
    private static final PhaseId LOCAL_SEARCH = new DefaultPhaseId(PhaseType.LOCAL_SEARCH, 1);

    @Test
    void message() {
        var termination = new DefaultTerminationId(TerminationType.SPENT_LIMIT, SOLVER_ROOT, 0, null);
        var runSummary = summary(false, termination, LOCAL_SEARCH);

        assertThat(SolverRunSummaryMessage.of("run-1", HardSoftScore.of(0, -1520), runSummary))
                .isEqualTo("Solver run (run-1) ended: outcome (SPENT_LIMIT at solver/termination), "
                        + "last phase (Local Search phase[1]), time spent (30000 ms), "
                        + "time to best solution (4500 ms, 15% of run), best score (0hard/-1520soft), "
                        + "solution initialized (true), move evaluations (8400000, 280000/sec), "
                        + "score calculations (9000000, 300000/sec), "
                        + "problem size (entities 412, variables 824, values ~1200, search space ~10^2310.4).");
    }

    @Test
    void messageWithoutBestScore() {
        assertThat(SolverRunSummaryMessage.of("run-1", null, summary(true, null, null)))
                .contains("outcome (terminated early)", "last phase (none)", "best score (null)");
    }

    @Test
    void outcomeOfTermination() {
        var termination = new DefaultTerminationId(TerminationType.BEST_SCORE_LIMIT, SOLVER_ROOT, 0, null);
        assertThat(SolverRunSummaryMessage.describeOutcome(summary(false, termination, LOCAL_SEARCH)))
                .isEqualTo("BEST_SCORE_LIMIT at solver/termination");
    }

    @Test
    void outcomeOfEarlyTermination() {
        assertThat(SolverRunSummaryMessage.describeOutcome(summary(true, null, LOCAL_SEARCH)))
                .isEqualTo("terminated early");
    }

    @Test
    void outcomeWithoutTermination() {
        assertThat(SolverRunSummaryMessage.describeOutcome(summary(false, null, LOCAL_SEARCH)))
                .isEqualTo("phases finished without a termination");
    }

    @Test
    void outcomeWithoutPhase() {
        assertThat(SolverRunSummaryMessage.describeOutcome(summary(false, null, null)))
                .isEqualTo("no phase ran");
    }

    @Test
    void locationOfTerminationInRootElement() {
        var termination = new DefaultTerminationId(TerminationType.SPENT_LIMIT, SOLVER_ROOT, 0, null);
        assertThat(SolverRunSummaryMessage.describeLocation(termination)).isEqualTo("solver/termination");
    }

    @Test
    void locationOfRootAndElement() {
        var termination = new DefaultTerminationId(TerminationType.AND, null, 0, null);
        assertThat(SolverRunSummaryMessage.describeLocation(termination)).isEqualTo("solver/termination");
    }

    @Test
    void locationOfTerminationInNestedElement() {
        var nested = new DefaultTerminationId(TerminationType.OR, SOLVER_ROOT, 0, null);
        var termination = new DefaultTerminationId(TerminationType.BEST_SCORE_LIMIT, nested, 0, null);
        assertThat(SolverRunSummaryMessage.describeLocation(termination))
                .isEqualTo("solver/termination/termination[0]");
    }

    @Test
    void locationOfNestedAndElement() {
        var termination = new DefaultTerminationId(TerminationType.AND, SOLVER_ROOT, 1, null);
        assertThat(SolverRunSummaryMessage.describeLocation(termination))
                .isEqualTo("solver/termination/termination[1]");
    }

    @Test
    void locationOfDeeplyNestedTermination() {
        var phaseRoot = new DefaultTerminationId(TerminationType.OR, null, 0, 0);
        var level1 = new DefaultTerminationId(TerminationType.OR, phaseRoot, 2, 0);
        var level2 = new DefaultTerminationId(TerminationType.OR, level1, 1, 0);
        var termination = new DefaultTerminationId(TerminationType.STEP_COUNT_LIMIT, level2, 0, 0);
        assertThat(SolverRunSummaryMessage.describeLocation(termination))
                .isEqualTo("phase[0]/termination/termination[2]/termination[1]");
    }

    @Test
    void locationOfPhaseTermination() {
        var phaseRoot = new DefaultTerminationId(TerminationType.OR, null, 0, 1);
        var termination = new DefaultTerminationId(TerminationType.UNIMPROVED_SPENT_LIMIT, phaseRoot, 0, 1);
        assertThat(SolverRunSummaryMessage.describeLocation(termination)).isEqualTo("phase[1]/termination");
    }

    @Test
    void phase() {
        assertThat(SolverRunSummaryMessage.describePhase(LOCAL_SEARCH)).isEqualTo("Local Search phase[1]");
        assertThat(SolverRunSummaryMessage.describePhase(new DefaultPhaseId(PhaseType.CONSTRUCTION_HEURISTIC, 0)))
                .isEqualTo("Construction Heuristic phase[0]");
        assertThat(SolverRunSummaryMessage.describePhase(null)).isEqualTo("none");
    }

    @Test
    void percent() {
        assertThat(SolverRunSummaryMessage.percentOf(Duration.ofSeconds(3), Duration.ofSeconds(4))).isEqualTo(75);
        assertThat(SolverRunSummaryMessage.percentOf(Duration.ZERO, Duration.ofSeconds(4))).isZero();
        assertThat(SolverRunSummaryMessage.percentOf(Duration.ZERO, Duration.ZERO)).isZero();
    }

    private static SolverRunSummary summary(boolean terminatedEarly, @Nullable TerminationId termination,
            @Nullable PhaseId lastPhase) {
        var problemSizeStatistics = new ProblemSizeStatistics(412, new LinkedHashMap<>(), 824, 1200,
                new LinkedHashMap<>(), 2310.44);
        return new DefaultSolverRunSummary(Duration.ofSeconds(30), 9_000_000, 8_400_000, true, problemSizeStatistics,
                terminatedEarly, termination, lastPhase, Duration.ofMillis(4500));
    }

}
