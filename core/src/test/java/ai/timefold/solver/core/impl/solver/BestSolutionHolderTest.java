package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.event.DefaultPhaseEventProducerId;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BestSolutionHolderTest {
    private static final Comparator<TestdataSolution> IDENTITY_COMPARATOR = (a, b) -> {
        if (a == b) {
            return 0;
        }
        return System.identityHashCode(a) < System.identityHashCode(b) ? -1 : 1;
    };

    @Test
    void setBestSolution() {
        var bestSolutionHolder = new BestSolutionHolder<TestdataSolution>();
        assertThat(bestSolutionHolder.take()).isNull();

        var solution1 = TestdataSolution.generateSolution();
        var solution2 = TestdataSolution.generateSolution();

        bestSolutionHolder.set(solution1, DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 0), () -> true);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution1, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 0),
                        from(BestSolutionContainingProblemChanges::getProducerId));
        assertThat(bestSolutionHolder.take()).isNull();

        bestSolutionHolder.set(solution1, DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 1), () -> true);
        bestSolutionHolder.set(solution2, DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, 2), () -> false);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution1, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 1),
                        from(BestSolutionContainingProblemChanges::getProducerId));

        bestSolutionHolder.set(solution1, DefaultPhaseEventProducerId.of(PhaseType.CUSTOM_PHASE, 3), () -> true);
        bestSolutionHolder.set(solution2, DefaultPhaseEventProducerId.of(PhaseType.CUSTOM_PHASE, 4), () -> true);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution2, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(DefaultPhaseEventProducerId.of(PhaseType.CUSTOM_PHASE, 4),
                        from(BestSolutionContainingProblemChanges::getProducerId));

    }

    @Test
    void completeProblemChanges() {
        var bestSolutionHolder = new BestSolutionHolder<TestdataSolution>();

        var problemChange1 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(),
                DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 0), () -> true);
        var problemChange2 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();
        assertThat(problemChange1).isCompleted();
        assertThat(problemChange2).isNotCompleted();

        var problemChange3 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(),
                DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 1), () -> true);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, 2),
                () -> true);
        var problemChange4 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();

        assertThat(problemChange2).isCompleted();
        assertThat(problemChange3).isCompleted();
        assertThat(problemChange4).isNotCompleted();
    }

    @Test
    void cancelPendingChanges_noChangesRetrieved() {
        var bestSolutionHolder = new BestSolutionHolder<TestdataSolution>();

        var problemChange = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(),
                DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, 0), () -> true);

        bestSolutionHolder.cancelPendingChanges();

        var bestSolution = bestSolutionHolder.take();
        bestSolution.completeProblemChanges();

        assertThat(problemChange).isCancelled();
    }

    private static CompletableFuture<Void> addProblemChange(BestSolutionHolder<TestdataSolution> bestSolutionHolder) {
        Solver<TestdataSolution> solver = mock(Solver.class);
        ProblemChange<TestdataSolution> problemChange = mock(ProblemChange.class);
        var futureChange = bestSolutionHolder.addProblemChange(solver, List.of(problemChange));
        verify(solver, times(1)).addProblemChanges(
                Mockito.argThat(problemChanges -> problemChanges.size() == 1 && problemChanges.get(0) == problemChange));
        return futureChange;
    }

}
