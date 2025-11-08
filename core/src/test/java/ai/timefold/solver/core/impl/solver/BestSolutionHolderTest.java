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
import ai.timefold.solver.core.api.solver.event.EventProducerId;
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
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();
        assertThat(bestSolutionHolder.take()).isNull();

        TestdataSolution solution1 = TestdataSolution.generateSolution();
        TestdataSolution solution2 = TestdataSolution.generateSolution();

        bestSolutionHolder.set(solution1, EventProducerId.constructionHeuristic(0), () -> true);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution1, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(EventProducerId.constructionHeuristic(0), from(BestSolutionContainingProblemChanges::getProducerId));
        assertThat(bestSolutionHolder.take()).isNull();

        bestSolutionHolder.set(solution1, EventProducerId.constructionHeuristic(1), () -> true);
        bestSolutionHolder.set(solution2, EventProducerId.localSearch(2), () -> false);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution1, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(EventProducerId.constructionHeuristic(1), from(BestSolutionContainingProblemChanges::getProducerId));

        bestSolutionHolder.set(solution1, EventProducerId.customPhase(3), () -> true);
        bestSolutionHolder.set(solution2, EventProducerId.customPhase(4), () -> true);
        assertThat(bestSolutionHolder.take())
                .usingComparatorForType(IDENTITY_COMPARATOR, TestdataSolution.class)
                .returns(solution2, from(BestSolutionContainingProblemChanges::getBestSolution))
                .returns(EventProducerId.customPhase(4), from(BestSolutionContainingProblemChanges::getProducerId));

    }

    @Test
    void completeProblemChanges() {
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();

        CompletableFuture<Void> problemChange1 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), EventProducerId.constructionHeuristic(0), () -> true);
        CompletableFuture<Void> problemChange2 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();
        assertThat(problemChange1).isCompleted();
        assertThat(problemChange2).isNotCompleted();

        CompletableFuture<Void> problemChange3 = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), EventProducerId.constructionHeuristic(1), () -> true);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), EventProducerId.localSearch(2), () -> true);
        CompletableFuture<Void> problemChange4 = addProblemChange(bestSolutionHolder);

        bestSolutionHolder.take().completeProblemChanges();

        assertThat(problemChange2).isCompleted();
        assertThat(problemChange3).isCompleted();
        assertThat(problemChange4).isNotCompleted();
    }

    @Test
    void cancelPendingChanges_noChangesRetrieved() {
        BestSolutionHolder<TestdataSolution> bestSolutionHolder = new BestSolutionHolder<>();

        CompletableFuture<Void> problemChange = addProblemChange(bestSolutionHolder);
        bestSolutionHolder.set(TestdataSolution.generateSolution(), EventProducerId.constructionHeuristic(0), () -> true);

        bestSolutionHolder.cancelPendingChanges();

        BestSolutionContainingProblemChanges<TestdataSolution> bestSolution = bestSolutionHolder.take();
        bestSolution.completeProblemChanges();

        assertThat(problemChange).isCancelled();
    }

    private static CompletableFuture<Void> addProblemChange(BestSolutionHolder<TestdataSolution> bestSolutionHolder) {
        Solver<TestdataSolution> solver = mock(Solver.class);
        ProblemChange<TestdataSolution> problemChange = mock(ProblemChange.class);
        CompletableFuture<Void> futureChange = bestSolutionHolder.addProblemChange(solver, List.of(problemChange));
        verify(solver, times(1)).addProblemChanges(
                Mockito.argThat(problemChanges -> problemChanges.size() == 1 && problemChanges.get(0) == problemChange));
        return futureChange;
    }

}
