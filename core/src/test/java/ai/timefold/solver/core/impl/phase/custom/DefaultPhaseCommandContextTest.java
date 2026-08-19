package ai.timefold.solver.core.impl.phase.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultPhaseCommandContextTest {

    Move<TestdataSolution> move;
    DefaultPhaseCommandContext<TestdataSolution> commandContext;
    InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector;
    Function<TestdataSolution, String> solutionConsumer;
    Function<TestdataSolution, String> flawedSolutionConsumer;

    private static final String SUCCESS = "success";
    private static final String ERROR = "error";

    @BeforeEach
    void setUp() {
        var solution = TestdataSolution.generateSolution(3, 2);
        move = mock(Move.class);
        scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);

        commandContext = new DefaultPhaseCommandContext<>(new MoveDirector<>(scoreDirector), () -> false);

        solutionConsumer = Mockito.mock(Function.class);
        when(solutionConsumer.apply(any())).thenReturn(SUCCESS);

        flawedSolutionConsumer = Mockito.mock(Function.class);
        when(flawedSolutionConsumer.apply(any())).thenReturn(ERROR);
    }

    @Test
    void executeTemporarilyHandlingStructurallyFlawedSolutions_moveDoesNotFlawSolution() {
        when(scoreDirector.calculateScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.of(0)));

        var result = commandContext
                .executeTemporarilyHandlingStructurallyFlawedSolutions(move,
                        solutionConsumer,
                        flawedSolutionConsumer);

        assertThat(result).isEqualTo(SUCCESS);
        verify(solutionConsumer, times(1)).apply(any());
        verify(flawedSolutionConsumer, never()).apply(any());
        verify(move, times(1)).execute(any(MoveDirector.class));
        verify(scoreDirector, times(1)).calculateScore();
    }

    @Test
    void executeTemporarilyHandlingStructurallyFlawedSolutions_moveFlawsSolution() {
        when(scoreDirector.calculateScore()).thenReturn(InnerScore.fullyAssigned(new SimpleScore(-1, 0)));

        var result = commandContext
                .executeTemporarilyHandlingStructurallyFlawedSolutions(move,
                        solutionConsumer,
                        flawedSolutionConsumer);

        assertThat(result).isEqualTo(ERROR);
        verify(solutionConsumer, never()).apply(any());
        verify(flawedSolutionConsumer, times(1)).apply(any());
        verify(move, times(1)).execute(any(MoveDirector.class));
        verify(scoreDirector, times(1)).calculateScore();
    }

    @Test
    void executeTemporarilyAndCalculateScoreHandlingStructurallyFlawedSolutions_moveDoesNotFlawSolution() {
        when(scoreDirector.calculateScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.of(0)));

        var result = commandContext
                .executeTemporarilyAndCalculateScoreHandlingStructurallyFlawedSolutions(move,
                        solutionConsumer,
                        flawedSolutionConsumer);

        assertThat(result).isEqualTo(SUCCESS);
        verify(solutionConsumer, times(1)).apply(any());
        verify(flawedSolutionConsumer, never()).apply(any());
        verify(move, times(1)).execute(any(MoveDirector.class));
        verify(scoreDirector, times(2)).calculateScore();
    }

    @Test
    void executeTemporarilyAndCalculateScoreHandlingStructurallyFlawedSolutions_moveFlawsSolution() {
        when(scoreDirector.calculateScore()).thenReturn(InnerScore.fullyAssigned(new SimpleScore(-1, 0)));

        var result = commandContext
                .executeTemporarilyAndCalculateScoreHandlingStructurallyFlawedSolutions(move,
                        solutionConsumer,
                        flawedSolutionConsumer);

        assertThat(result).isEqualTo(ERROR);
        verify(solutionConsumer, never()).apply(any());
        verify(flawedSolutionConsumer, times(1)).apply(any());
        verify(move, times(1)).execute(any(MoveDirector.class));
        verify(scoreDirector, times(2)).calculateScore();
    }

}
