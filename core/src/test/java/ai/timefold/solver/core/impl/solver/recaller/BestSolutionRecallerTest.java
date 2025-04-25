package ai.timefold.solver.core.impl.solver.recaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BestSolutionRecallerTest {

    private static <Solution_> SolverScope<Solution_> createSolverScope() {
        var solverScope = new SolverScope<Solution_>();
        InnerScoreDirector<Solution_, ?> scoreDirector = mock(InnerScoreDirector.class);
        SolutionDescriptor<Solution_> solutionDescriptor = mock(SolutionDescriptor.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        solverScope.setScoreDirector(scoreDirector);
        return solverScope;
    }

    private static <Solution_> ConstructionHeuristicStepScope<Solution_> setupConstructionHeuristics(
            SolverScope<Solution_> solverScope) {
        ConstructionHeuristicPhaseScope<Solution_> phaseScope = mock(ConstructionHeuristicPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        ConstructionHeuristicStepScope<Solution_> stepScope = mock(ConstructionHeuristicStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(stepScope.getScoreDirector()).thenReturn(solverScope.getScoreDirector());
        return stepScope;
    }

    private static <Solution_> BestSolutionRecaller<Solution_> createBestSolutionRecaller() {
        var recaller = new BestSolutionRecaller<Solution_>();
        recaller.setSolverEventSupport(mock(SolverEventSupport.class));
        return recaller;
    }

    @Test
    void unimprovedUninitializedProcessWorkingSolutionDuringStep() {
        var originalBestScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class),
                        InnerScore.withUnassignedCount(SimpleScore.of(-300), 1));
        var stepScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.withUnassignedCount(SimpleScore.ZERO, 2));
        doProcessWorkingSolutionDuringStep(originalBestScore, stepScore, false);
    }

    @Test
    void unimprovedInitializedProcessWorkingSolutionDuringStep() {
        var originalBestScore = SimpleScore.of(0);
        var stepScore = SimpleScore.of(-1);
        doProcessWorkingSolutionDuringStep(originalBestScore, stepScore, false);
    }

    @Test
    void improvedUninitializedProcessWorkingSolutionDuringStep() {
        var originalBestScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.withUnassignedCount(SimpleScore.ZERO, 2));
        var stepScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.withUnassignedCount(SimpleScore.ZERO, 1));
        doProcessWorkingSolutionDuringStep(originalBestScore, stepScore, true);
    }

    @Test
    void improvedInitializedProcessWorkingSolutionDuringStep() {
        var originalBestScore = SimpleScore.of(-1);
        var stepScore = SimpleScore.of(0);
        doProcessWorkingSolutionDuringStep(originalBestScore, stepScore, true);
    }

    protected void doProcessWorkingSolutionDuringStep(InnerScoreWithSolution<TestdataSolution, SimpleScore> originalBestScore,
            InnerScoreWithSolution<TestdataSolution, SimpleScore> stepScore, boolean stepImprovesBestSolution) {
        SolverScope<TestdataSolution> solverScope = createSolverScope();
        var originalBestSolution = originalBestScore.solution;
        var scoreDirector = solverScope.getScoreDirector();
        when(scoreDirector.getSolutionDescriptor().getScore(originalBestSolution))
                .thenReturn(originalBestScore.innerScore.raw());
        when(scoreDirector.getWorkingInitScore()).thenReturn(-originalBestScore.innerScore.unassignedCount());
        solverScope.setBestSolution(originalBestSolution);
        solverScope.setBestScore(originalBestScore.innerScore);

        var stepScope = setupConstructionHeuristics(solverScope);
        var stepSolution = stepScore.solution;
        when(scoreDirector.getSolutionDescriptor().getScore(stepSolution))
                .thenReturn(stepScore.innerScore.raw());
        when(scoreDirector.getWorkingInitScore()).thenReturn(-stepScore.innerScore.unassignedCount());
        doReturn(stepScore.innerScore).when(stepScope).getScore();
        when(stepScope.createOrGetClonedSolution()).thenReturn(stepSolution);

        BestSolutionRecaller<TestdataSolution> recaller = createBestSolutionRecaller();
        recaller.processWorkingSolutionDuringStep(stepScope);
        if (stepImprovesBestSolution) {
            assertThat(solverScope.getBestSolution()).isEqualTo(stepSolution);
            assertThat(solverScope.getBestScore()).isEqualTo(stepScore.innerScore);
        } else {
            assertThat(solverScope.getBestSolution()).isEqualTo(originalBestSolution);
            assertThat(solverScope.getBestScore()).isEqualTo(originalBestScore.innerScore);
        }
    }

    record InnerScoreWithSolution<Solution_, Score_ extends Score<Score_>>(Solution_ solution, InnerScore<Score_> innerScore) {

    }

    protected void doProcessWorkingSolutionDuringStep(SimpleScore originalBestScore, SimpleScore stepScore,
            boolean stepImprovesBestSolution) {
        var originalBestInnerScore = new InnerScoreWithSolution<TestdataSolution, SimpleScore>(mock(TestdataSolution.class),
                InnerScore.fullyAssigned(originalBestScore));
        var originalStepInnerScore = new InnerScoreWithSolution<TestdataSolution, SimpleScore>(mock(TestdataSolution.class),
                InnerScore.fullyAssigned(stepScore));
        doProcessWorkingSolutionDuringStep(originalBestInnerScore, originalStepInnerScore, stepImprovesBestSolution);
    }

    @Test
    void unimprovedUninitializedProcessWorkingSolutionDuringMove() {
        var bestScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.fullyAssigned(SimpleScore.of(-10)));
        var moveScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class),
                        InnerScore.withUnassignedCount(SimpleScore.of(-1), 1));
        doProcessWorkingSolutionDuringMove(bestScore, moveScore, false);
    }

    @Test
    void unimprovedInitializedProcessWorkingSolutionDuringMove() {
        var bestScore = SimpleScore.of(0);
        var moveScore = SimpleScore.of(-1);
        doProcessWorkingSolutionDuringMove(bestScore, moveScore, false);
    }

    @Test
    void improvedUninitializedProcessWorkingSolutionDuringMove() {
        var bestScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.withUnassignedCount(SimpleScore.ZERO, 1));
        var moveScore =
                new InnerScoreWithSolution<>(mock(TestdataSolution.class), InnerScore.fullyAssigned(SimpleScore.of(-2)));
        doProcessWorkingSolutionDuringMove(bestScore, moveScore, true);
    }

    @Test
    void improvedInitializedProcessWorkingSolutionDuringMove() {
        var bestScore = SimpleScore.of(-2);
        var moveScore = SimpleScore.of(-1);
        doProcessWorkingSolutionDuringMove(bestScore, moveScore, true);
    }

    protected void doProcessWorkingSolutionDuringMove(SimpleScore originalBestScore, SimpleScore moveScore,
            boolean moveImprovesBestSolution) {
        var originalBestInnerScore = new InnerScoreWithSolution<TestdataSolution, SimpleScore>(mock(TestdataSolution.class),
                InnerScore.fullyAssigned(originalBestScore));
        var originalMoveInnerScore = new InnerScoreWithSolution<TestdataSolution, SimpleScore>(mock(TestdataSolution.class),
                InnerScore.fullyAssigned(moveScore));
        doProcessWorkingSolutionDuringMove(originalBestInnerScore, originalMoveInnerScore, moveImprovesBestSolution);
    }

    protected void doProcessWorkingSolutionDuringMove(InnerScoreWithSolution<TestdataSolution, SimpleScore> originalBestScore,
            InnerScoreWithSolution<TestdataSolution, SimpleScore> moveScore,
            boolean moveImprovesBestSolution) {
        SolverScope<TestdataSolution> solverScope = createSolverScope();
        var originalBestSolution = originalBestScore.solution;
        when(solverScope.getScoreDirector().getSolutionDescriptor().getScore(originalBestSolution))
                .thenReturn(originalBestScore.innerScore.raw());
        solverScope.setBestSolution(originalBestSolution);
        solverScope.setBestScore(originalBestScore.innerScore);

        var stepScope = setupConstructionHeuristics(solverScope);

        var moveSolution = moveScore.solution;
        when(solverScope.getScoreDirector().getSolutionDescriptor().getScore(moveSolution))
                .thenReturn(moveScore.innerScore.raw());
        when(solverScope.getScoreDirector().cloneWorkingSolution()).thenReturn(moveSolution);

        BestSolutionRecaller<TestdataSolution> recaller = createBestSolutionRecaller();
        recaller.processWorkingSolutionDuringMove(moveScore.innerScore, stepScope);
        if (moveImprovesBestSolution) {
            assertThat(solverScope.getBestSolution()).isEqualTo(moveSolution);
            assertThat(solverScope.getBestScore()).isEqualTo(moveScore.innerScore);
        } else {
            assertThat(solverScope.getBestSolution()).isEqualTo(originalBestSolution);
            assertThat(solverScope.getBestScore()).isEqualTo(originalBestScore.innerScore);
        }
    }

}
