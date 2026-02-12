package ai.timefold.solver.core.impl.score.director.incremental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;

import org.junit.jupiter.api.Test;

class IncrementalScoreDirectorTest {

    @Test
    void illegalStateExceptionThrownWhenConstraintMatchNotEnabled() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(false))
                .build()) {
            scoreDirector.setWorkingSolution(new Object());
            assertThatIllegalStateException()
                    .isThrownBy(scoreDirector::getConstraintMatchTotalMap)
                    .withMessageContaining(ConstraintMatchPolicy.DISABLED.name());
        }
    }

    @Test
    void constraintMatchTotalsNeverNull() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(true))
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .build()) {
            scoreDirector.setWorkingSolution(new Object());
            assertThat(scoreDirector.getConstraintMatchTotalMap()).isNotNull();
        }
    }

    @Test
    void constraintMatchIsNotEnabledWhenScoreCalculatorNotConstraintMatchAware() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(false))
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .build()) {
            assertThat(scoreDirector.getConstraintMatchPolicy()).isEqualTo(ConstraintMatchPolicy.DISABLED);
        }
    }

    @SuppressWarnings("unchecked")
    private IncrementalScoreDirectorFactory<Object, SimpleScore> mockIncrementalScoreDirectorFactory() {
        IncrementalScoreDirectorFactory<Object, SimpleScore> factory = mock(IncrementalScoreDirectorFactory.class);
        when(factory.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        SolutionDescriptor<Object> solutionDescriptor = mock(SolutionDescriptor.class);
        when(factory.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        return factory;
    }

    @SuppressWarnings("unchecked")
    private IncrementalScoreCalculator<Object, SimpleScore> mockIncrementalScoreCalculator(boolean constraintMatchAware) {
        return constraintMatchAware
                ? mock(ConstraintMatchAwareIncrementalScoreCalculator.class)
                : mock(IncrementalScoreCalculator.class);
    }
}
