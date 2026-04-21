package ai.timefold.solver.core.impl.score.director.incremental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.AnalyzableIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchRegistration;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchRegistry;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IncrementalScoreDirectorTest {

    private static final ConstraintRef CONSTRAINT_A = ConstraintRef.of("constraintA");
    private static final ConstraintRef CONSTRAINT_B = ConstraintRef.of("constraintB");

    @Test
    void illegalStateExceptionThrownWhenConstraintMatchNotEnabled() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(false)).build()) {
            scoreDirector.setWorkingSolution(new Object());
            assertThatIllegalStateException().isThrownBy(scoreDirector::getConstraintMatchTotalMap)
                    .withMessageContaining(ConstraintMatchPolicy.DISABLED.name());
        }
    }

    @Test
    void constraintMatchTotalsNeverNull() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(true))
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED).build()) {
            scoreDirector.setWorkingSolution(new Object());
            assertThat(scoreDirector.getConstraintMatchTotalMap()).isNotNull();
        }
    }

    @Test
    void constraintMatchIsNotEnabledWhenScoreCalculatorNotConstraintMatchAware() {
        try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(false))
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED).build()) {
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
        if (constraintMatchAware) {
            return mock(AnalyzableIncrementalScoreCalculator.class);
        } else {
            return mock(IncrementalScoreCalculator.class);
        }
    }

    @Nested
    class Justifications {

        @Test
        void registerConstraintMatchThrowsWhenConstraintMatchingDisabled() {
            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(mockIncrementalScoreCalculator(false)).build()) {
                scoreDirector.setWorkingSolution(new Object());
                assertThatIllegalStateException()
                        .isThrownBy(() -> scoreDirector.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-1),
                                DefaultConstraintJustification.of(SimpleScore.of(-1))))
                        .withMessageContaining(ConstraintMatchPolicy.DISABLED.name());
            }
        }

        @Test
        void registerConstraintMatchUpdatesTotalScoreAndMap() {
            var calculator = new RegistryCapturingCalculator(SimpleScore.of(-5));
            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());

                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.of(-5));
                var matchTotalMap = scoreDirector.getConstraintMatchTotalMap();
                assertThat(matchTotalMap).containsKey(CONSTRAINT_A);
                assertThat(matchTotalMap.get(CONSTRAINT_A).getScore()).isEqualTo(SimpleScore.of(-5));
                assertThat(matchTotalMap.get(CONSTRAINT_A).getConstraintMatchSet()).hasSize(1);
            }
        }

        @Test
        void cancelRegistrationUpdatesTotalScoreAndMap() {
            var registration = new ConstraintMatchRegistration[1];
            var calculator = new AnalyzableIncrementalScoreCalculator<Object, SimpleScore>() {

                private ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

                @Override
                public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                    this.constraintMatchRegistry = constraintMatchRegistry;
                }

                @Override
                public void resetWorkingSolution(Object workingSolution) {
                    registration[0] = constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-3),
                            DefaultConstraintJustification.of(SimpleScore.of(-3)));
                }

                @Override
                public void beforeVariableChanged(Object entity, String variableName) {
                }

                @Override
                public void afterVariableChanged(Object entity, String variableName) {
                }

                @Override
                public SimpleScore calculateScore() {
                    return SimpleScore.of(-3);
                }
            };

            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());
                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.of(-3));

                registration[0].cancel();
                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.ZERO);
                assertThat(scoreDirector.getConstraintMatchTotalMap().get(CONSTRAINT_A).getConstraintMatchSet()).isEmpty();
            }
        }

        @Test
        void cancelRegistrationTwiceThrows() {
            var registration = new ConstraintMatchRegistration[1];
            var calculator = new AnalyzableIncrementalScoreCalculator<Object, SimpleScore>() {

                private ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

                @Override
                public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                    this.constraintMatchRegistry = constraintMatchRegistry;
                }

                @Override
                public void resetWorkingSolution(Object workingSolution) {
                    registration[0] = constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-1),
                            DefaultConstraintJustification.of(SimpleScore.of(-1)));
                }

                @Override
                public void beforeVariableChanged(Object entity, String variableName) {
                }

                @Override
                public void afterVariableChanged(Object entity, String variableName) {
                }

                @Override
                public SimpleScore calculateScore() {
                    return SimpleScore.of(-1);
                }
            };

            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());
                registration[0].cancel();
                assertThatIllegalStateException().isThrownBy(() -> registration[0].cancel())
                        .withMessageContaining("canceled once");
            }
        }

        @Test
        void multipleConstraintMatchesAcrossDifferentConstraints() {
            var calculator = new AnalyzableIncrementalScoreCalculator<Object, SimpleScore>() {

                private ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

                @Override
                public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                    this.constraintMatchRegistry = constraintMatchRegistry;
                }

                @Override
                public void resetWorkingSolution(Object workingSolution) {
                    constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-2),
                            DefaultConstraintJustification.of(SimpleScore.of(-2)));
                    constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-3),
                            DefaultConstraintJustification.of(SimpleScore.of(-3)));
                    constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_B, SimpleScore.of(-5),
                            DefaultConstraintJustification.of(SimpleScore.of(-5)));
                }

                @Override
                public void beforeVariableChanged(Object entity, String variableName) {
                }

                @Override
                public void afterVariableChanged(Object entity, String variableName) {
                }

                @Override
                public SimpleScore calculateScore() {
                    return SimpleScore.of(-10);
                }
            };

            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());

                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.of(-10));
                var matchTotalMap = scoreDirector.getConstraintMatchTotalMap();
                assertThat(matchTotalMap).containsKeys(CONSTRAINT_A, CONSTRAINT_B);
                assertThat(matchTotalMap.get(CONSTRAINT_A).getConstraintMatchSet()).hasSize(2);
                assertThat(matchTotalMap.get(CONSTRAINT_A).getScore()).isEqualTo(SimpleScore.of(-5));
                assertThat(matchTotalMap.get(CONSTRAINT_B).getConstraintMatchSet()).hasSize(1);
                assertThat(matchTotalMap.get(CONSTRAINT_B).getScore()).isEqualTo(SimpleScore.of(-5));
            }
        }

        @Test
        void resetClearsConstraintMatchMapAndResetsScore() {
            var callCount = new int[1];
            var calculator = new AnalyzableIncrementalScoreCalculator<Object, SimpleScore>() {

                private ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

                @Override
                public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                    this.constraintMatchRegistry = constraintMatchRegistry;
                }

                @Override
                public void resetWorkingSolution(Object workingSolution) {
                    callCount[0]++;
                    // First reset: register a match; subsequent resets: register nothing.
                    if (callCount[0] == 1) {
                        constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-7),
                                DefaultConstraintJustification.of(SimpleScore.of(-7)));
                    }
                }

                @Override
                public void beforeVariableChanged(Object entity, String variableName) {
                }

                @Override
                public void afterVariableChanged(Object entity, String variableName) {
                }

                @Override
                public SimpleScore calculateScore() {
                    return SimpleScore.ZERO;
                }
            };

            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());
                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.of(-7));
                assertThat(scoreDirector.getConstraintMatchTotalMap()).containsKey(CONSTRAINT_A);

                // Trigger a second reset via setWorkingSolution.
                scoreDirector.setWorkingSolution(new Object());
                assertThat(scoreDirector.totalScore()).isEqualTo(SimpleScore.ZERO);
                assertThat(scoreDirector.getConstraintMatchTotalMap()).doesNotContainKey(CONSTRAINT_A);
            }
        }

        @Test
        void registrationConstraintRefAndScoreAreAccessible() {
            var registration = new ConstraintMatchRegistration[1];
            var calculator = new AnalyzableIncrementalScoreCalculator<Object, SimpleScore>() {

                private ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

                @Override
                public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                    this.constraintMatchRegistry = constraintMatchRegistry;
                }

                @Override
                public void resetWorkingSolution(Object workingSolution) {
                    var justification = DefaultConstraintJustification.of(SimpleScore.of(-4));
                    registration[0] =
                            constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, SimpleScore.of(-4), justification);
                }

                @Override
                public void beforeVariableChanged(Object entity, String variableName) {
                }

                @Override
                public void afterVariableChanged(Object entity, String variableName) {
                }

                @Override
                public SimpleScore calculateScore() {
                    return SimpleScore.of(-4);
                }
            };

            try (var scoreDirector = new IncrementalScoreDirector.Builder<>(mockIncrementalScoreDirectorFactory())
                    .withIncrementalScoreCalculator(calculator).withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                    .build()) {
                scoreDirector.setWorkingSolution(new Object());

                assertThat(registration[0].constraintRef()).isEqualTo(CONSTRAINT_A);
                assertThat(registration[0].score()).isEqualTo(SimpleScore.of(-4));
                assertThat(registration[0].justification()).isNotNull();
            }
        }

        @NullMarked
        private static class RegistryCapturingCalculator
                implements AnalyzableIncrementalScoreCalculator<Object, SimpleScore> {

            private final SimpleScore scoreToRegister;
            private @Nullable ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;

            public RegistryCapturingCalculator(SimpleScore scoreToRegister) {
                this.scoreToRegister = scoreToRegister;
            }

            @Override
            public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
                this.constraintMatchRegistry = constraintMatchRegistry;
            }

            @Override
            public void resetWorkingSolution(Object workingSolution) {
                constraintMatchRegistry.registerConstraintMatch(CONSTRAINT_A, scoreToRegister,
                        DefaultConstraintJustification.of(scoreToRegister));
            }

            @Override
            public void beforeVariableChanged(Object entity, String variableName) {
            }

            @Override
            public void afterVariableChanged(Object entity, String variableName) {
            }

            @Override
            public SimpleScore calculateScore() {
                return scoreToRegister;
            }
        }

    }
}
