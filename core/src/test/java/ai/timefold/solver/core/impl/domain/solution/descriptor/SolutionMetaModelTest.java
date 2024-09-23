package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ShadowVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.SolutionMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SolutionMetaModelTest {

    @Nested
    @DisplayName("Solution with a basic variable")
    class BasicVariableSolutionMetaModelTest {

        private final SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        private final SolutionMetaModel<TestdataSolution> solutionMetaModel = solutionDescriptor.getMetaModel();

        @Test
        void hasProperType() {
            assertThat(solutionMetaModel.type())
                    .isEqualTo(TestdataSolution.class);
        }

        @Test
        void hasProperEntities() {
            assertThat(solutionMetaModel.entities())
                    .containsOnly(solutionMetaModel.entity(TestdataEntity.class));
        }

        @Test
        void hasProperGenuineEntities() {
            assertThat(solutionMetaModel.genuineEntities())
                    .containsOnly(solutionMetaModel.entity(TestdataEntity.class));
        }

        @Test
        void failsOnWrongEntities() {
            assertSoftly(softly -> {
                softly.assertThatThrownBy(() -> solutionMetaModel.entity(TestdataListEntity.class))
                        .hasMessageContaining(TestdataListEntity.class.getSimpleName());
                softly.assertThatThrownBy(() -> solutionMetaModel.entity(TestdataValue.class))
                        .hasMessageContaining(TestdataValue.class.getSimpleName());
            });
        }

        @Nested
        @DisplayName("with a genuine entity")
        class BasicVariableEntityMetaModelTest {

            private final EntityMetaModel<TestdataSolution, TestdataEntity> entityMetaModel =
                    solutionMetaModel.entity(TestdataEntity.class);

            @Test
            void hasProperParent() {
                assertThat(entityMetaModel.solution())
                        .isSameAs(solutionMetaModel);
            }

            @Test
            void hasProperType() {
                assertThat(entityMetaModel.type())
                        .isEqualTo(TestdataEntity.class);
            }

            @Test
            void isGenuine() {
                assertThat(entityMetaModel.isGenuine())
                        .isTrue();
            }

            @Test
            void hasProperVariables() {
                var variableMetaModel = entityMetaModel.variable("value");
                assertSoftly(softly -> {
                    softly.assertThat(entityMetaModel.variables())
                            .containsOnly(variableMetaModel);
                    softly.assertThat(entityMetaModel.genuineVariables())
                            .containsOnly(variableMetaModel);
                });
            }

            @Test
            void failsOnNonExistingVariable() {
                assertThatThrownBy(() -> entityMetaModel.variable("nonExisting"))
                        .hasMessageContaining("nonExisting");
            }

            @Nested
            @DisplayName("with a genuine variable")
            class BasicVariableMetaModelTest {

                private final VariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variableMetaModel =
                        entityMetaModel.variable("value");

                @Test
                void hasProperParent() {
                    assertThat(variableMetaModel.entity())
                            .isSameAs(entityMetaModel);
                }

                @Test
                void hasProperType() {
                    assertThat(variableMetaModel.type())
                            .isEqualTo(TestdataValue.class);
                }

                @Test
                void isGenuine() {
                    assertThat(variableMetaModel.isGenuine())
                            .isTrue();
                }

                @Test
                void isNotList() {
                    assertThat(variableMetaModel.isList())
                            .isFalse();
                }

            }

        }
    }

    @Nested
    @DisplayName("Solution with a list variable")
    class ListVariableSolutionMetaModelTest {

        private final SolutionDescriptor<TestdataListSolution> solutionDescriptor =
                TestdataListSolution.buildSolutionDescriptor();
        private final SolutionMetaModel<TestdataListSolution> solutionMetaModel = solutionDescriptor.getMetaModel();

        @Test
        void hasProperType() {
            assertThat(solutionMetaModel.type())
                    .isEqualTo(TestdataListSolution.class);
        }

        @Test
        void hasProperEntities() {
            assertThat(solutionMetaModel.entities())
                    .containsExactly(
                            solutionMetaModel.entity(TestdataListEntity.class),
                            solutionMetaModel.entity(TestdataListValue.class));
        }

        @Test
        void hasProperGenuineEntities() {
            assertThat(solutionMetaModel.genuineEntities())
                    .containsOnly(solutionMetaModel.entity(TestdataListEntity.class));
        }

        @Test
        void failsOnWrongEntities() {
            assertSoftly(softly -> {
                softly.assertThatThrownBy(() -> solutionMetaModel.entity(TestdataEntity.class))
                        .hasMessageContaining(TestdataEntity.class.getSimpleName());
                softly.assertThatThrownBy(() -> solutionMetaModel.entity(TestdataValue.class))
                        .hasMessageContaining(TestdataValue.class.getSimpleName());
            });
        }

        @Nested
        @DisplayName("with a genuine entity")
        class GenuineEntityMetaModelTest {

            private final EntityMetaModel<TestdataListSolution, TestdataListEntity> entityMetaModel =
                    solutionMetaModel.entity(TestdataListEntity.class);

            @Test
            void hasProperParent() {
                assertThat(entityMetaModel.solution())
                        .isSameAs(solutionMetaModel);
            }

            @Test
            void hasProperType() {
                assertThat(entityMetaModel.type())
                        .isEqualTo(TestdataListEntity.class);
            }

            @Test
            void isGenuine() {
                assertThat(entityMetaModel.isGenuine())
                        .isTrue();
            }

            @Test
            void hasProperVariables() {
                var variableMetaModel = entityMetaModel.variable("valueList");
                assertSoftly(softly -> {
                    softly.assertThat(entityMetaModel.variables())
                            .containsOnly(variableMetaModel);
                    softly.assertThat(entityMetaModel.genuineVariables())
                            .containsOnly(variableMetaModel);
                });
            }

            @Test
            void failsOnNonExistingVariable() {
                assertThatThrownBy(() -> entityMetaModel.variable("nonExisting"))
                        .hasMessageContaining("nonExisting");
            }

            @Nested
            @DisplayName("with a genuine variable")
            class ListVariableMetaModelTest {

                private final ListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue> variableMetaModel =
                        (ListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue>) entityMetaModel
                                .<TestdataListValue> variable("valueList");

                @Test
                void hasProperParent() {
                    assertThat(variableMetaModel.entity())
                            .isSameAs(entityMetaModel);
                }

                @Test
                void hasProperType() {
                    assertThat(variableMetaModel.type())
                            .isEqualTo(TestdataListValue.class);
                }

                @Test
                void isGenuine() {
                    assertThat(variableMetaModel.isGenuine())
                            .isTrue();
                }

                @Test
                void isList() {
                    assertThat(variableMetaModel.isList())
                            .isTrue();
                }

            }

        }

        @Nested
        @DisplayName("with a shadow entity")
        class ShadowEntityMetaModelTest {

            private final EntityMetaModel<TestdataListSolution, TestdataListValue> entityMetaModel =
                    solutionMetaModel.entity(TestdataListValue.class);

            @Test
            void hasProperParent() {
                assertThat(entityMetaModel.solution())
                        .isSameAs(solutionMetaModel);
            }

            @Test
            void hasProperType() {
                assertThat(entityMetaModel.type())
                        .isEqualTo(TestdataListValue.class);
            }

            @Test
            void isNotGenuine() {
                assertThat(entityMetaModel.isGenuine())
                        .isFalse();
            }

            @Test
            void hasProperVariables() {
                var genuineVariableMetaModel = entityMetaModel.<TestdataListEntity> variable("entity");
                var shadowVariableMetaModel = entityMetaModel.<Integer> variable("index");
                assertSoftly(softly -> {
                    softly.assertThat(entityMetaModel.variables())
                            .containsExactly(genuineVariableMetaModel, shadowVariableMetaModel);
                    softly.assertThat(entityMetaModel.genuineVariables())
                            .isEmpty();
                });
            }

            @Test
            void failsOnNonExistingVariable() {
                assertThatThrownBy(() -> entityMetaModel.variable("nonExisting"))
                        .hasMessageContaining("nonExisting");
            }

            @Nested
            @DisplayName("with a shadow variable")
            class ListVariableMetaModelTest {

                private final ShadowVariableMetaModel<TestdataListSolution, TestdataListValue, TestdataListEntity> variableMetaModel =
                        (ShadowVariableMetaModel<TestdataListSolution, TestdataListValue, TestdataListEntity>) entityMetaModel
                                .<TestdataListEntity> variable("entity");

                @Test
                void hasProperParent() {
                    assertThat(variableMetaModel.entity())
                            .isSameAs(entityMetaModel);
                }

                @Test
                void hasProperType() {
                    assertThat(variableMetaModel.type())
                            .isEqualTo(TestdataListEntity.class);
                }

                @Test
                void isNotGenuine() {
                    assertThat(variableMetaModel.isGenuine())
                            .isFalse();
                }

                @Test
                void isList() {
                    assertThat(variableMetaModel.isList())
                            .isFalse();
                }

            }

        }
    }

}
