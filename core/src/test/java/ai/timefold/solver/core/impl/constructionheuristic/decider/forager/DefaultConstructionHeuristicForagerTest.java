package ai.timefold.solver.core.impl.constructionheuristic.decider.forager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicPickEarlyType;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicMoveScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class DefaultConstructionHeuristicForagerTest<Solution_> {

    @Test
    void checkPickEarlyNever() {
        DefaultConstructionHeuristicForager forager = new DefaultConstructionHeuristicForager(
                ConstructionHeuristicPickEarlyType.NEVER);
        ConstructionHeuristicStepScope<Solution_> stepScope =
                buildStepScope(InnerScore.ofUninitialized(SimpleScore.of(-100), 8));
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(SimpleScore.of(-110), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(SimpleScore.of(-100), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(SimpleScore.of(-90), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
    }

    @Test
    void checkPickEarlyFirstNonDeterioratingScore() {
        DefaultConstructionHeuristicForager forager = new DefaultConstructionHeuristicForager(
                ConstructionHeuristicPickEarlyType.FIRST_NON_DETERIORATING_SCORE);
        ConstructionHeuristicStepScope<Solution_> stepScope =
                buildStepScope(InnerScore.ofUninitialized(SimpleScore.of(-100), 8));
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(SimpleScore.of(-110), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(SimpleScore.of(-100), 7)));
        assertThat(forager.isQuitEarly()).isTrue();
    }

    @Test
    void checkPickEarlyFirstFeasibleScore() {
        DefaultConstructionHeuristicForager forager = new DefaultConstructionHeuristicForager(
                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE);
        ConstructionHeuristicStepScope<Solution_> stepScope =
                buildStepScope(InnerScore.ofUninitialized(HardSoftScore.of(0, -100), 8));
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(-1, -110), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(-1, -90), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(0, -110), 7)));
        assertThat(forager.isQuitEarly()).isTrue();
    }

    @Test
    void checkPickEarlyFirstFeasibleScoreOrNonDeterioratingHard() {
        DefaultConstructionHeuristicForager forager = new DefaultConstructionHeuristicForager(
                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD);
        ConstructionHeuristicStepScope<Solution_> stepScope =
                buildStepScope(InnerScore.ofUninitialized(HardSoftScore.of(-10, -100), 8));
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(-11, -110), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(-11, -90), 7)));
        assertThat(forager.isQuitEarly()).isFalse();
        forager.checkPickEarly(buildMoveScope(stepScope, InnerScore.ofUninitialized(HardSoftScore.of(-10, -110), 7)));
        assertThat(forager.isQuitEarly()).isTrue();
    }

    protected ConstructionHeuristicStepScope<Solution_> buildStepScope(InnerScore<?> lastStepScore) {
        ConstructionHeuristicPhaseScope<Solution_> phaseScope = mock(ConstructionHeuristicPhaseScope.class);
        ConstructionHeuristicStepScope<Solution_> lastCompletedStepScope = mock(ConstructionHeuristicStepScope.class);
        when(lastCompletedStepScope.getPhaseScope()).thenReturn(phaseScope);
        doReturn(lastStepScore).when(lastCompletedStepScope).getScore();
        when(phaseScope.getLastCompletedStepScope()).thenReturn(lastCompletedStepScope);

        ConstructionHeuristicStepScope<Solution_> stepScope = mock(ConstructionHeuristicStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        return stepScope;
    }

    protected ConstructionHeuristicMoveScope<Solution_> buildMoveScope(ConstructionHeuristicStepScope<Solution_> stepScope,
            InnerScore<?> score) {
        ConstructionHeuristicMoveScope<Solution_> moveScope = mock(ConstructionHeuristicMoveScope.class);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        doReturn(score).when(moveScope).getScore();
        return moveScope;
    }

}
