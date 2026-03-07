package ai.timefold.solver.migration.v1;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class SortingMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new FindMissingTypes(false), new SortingMigrationRecipe())
                //                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.domain.entity;

                                        public @interface PlanningEntity {
                                            Class difficultyComparatorClass();
                                            Class difficultyWeightFactoryClass();
                                            interface NullDifficultyWeightFactory {
                                            }
                                            interface NullDifficultyComparator {
                                            }
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.domain.variable;

                                        public @interface PlanningVariable {
                                            Class strengthComparatorClass();
                                            Class strengthWeightFactoryClass();
                                            interface NullStrengthWeightFactory {
                                            }
                                            interface NullStrengthComparator {
                                            }
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.move;

                                        public interface MoveSelectorConfig {
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.move.generic;
                                        import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;

                                        public class ChangeMoveSelectorConfig implements MoveSelectorConfig {
                                           public void withSorterWeightFactoryClass(Class parameter) {}
                                           public void withSorterComparatorClass(Class parameter) {}
                                           public void setSorterWeightFactoryClass(Class parameter) {}
                                           public void setSorterComparatorClass(Class parameter) {}
                                           public void getSorterWeightFactoryClass() {}
                                           public void getSorterComparatorClass() {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;
                                        import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;

                                        public class ListSwapMoveSelectorConfig implements MoveSelectorConfig {
                                           public void withSorterWeightFactoryClass(Class parameter) {}
                                           public void withSorterComparatorClass(Class parameter) {}
                                           public void setSorterWeightFactoryClass(Class parameter) {}
                                           public void setSorterComparatorClass(Class parameter) {}
                                           public void getSorterWeightFactoryClass() {}
                                           public void getSorterComparatorClass() {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.entity;

                                        public class EntitySelectorConfig {
                                           public void withSorterWeightFactoryClass(Class parameter) {}
                                           public void withSorterComparatorClass(Class parameter) {}
                                           public void setSorterWeightFactoryClass(Class parameter) {}
                                           public void setSorterComparatorClass(Class parameter) {}
                                           public void getSorterWeightFactoryClass() {}
                                           public void getSorterComparatorClass() {}
                                           public void setSorterManner(Object parameter) {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.value;

                                        public class ValueSelectorConfig {
                                           public void withSorterWeightFactoryClass(Class parameter) {}
                                           public void withSorterComparatorClass(Class parameter) {}
                                           public void setSorterWeightFactoryClass(Class parameter) {}
                                           public void setSorterComparatorClass(Class parameter) {}
                                           public void getSorterWeightFactoryClass() {}
                                           public void getSorterComparatorClass() {}
                                           public void setSorterManner(Object parameter) {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

                                        public class SelectionSorterWeightFactory {
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.entity;

                                        public enum EntitySorterManner {
                                           DECREASING_DIFFICULTY_IF_AVAILABLE, DECREASING_DIFFICULTY
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.value;

                                        public enum ValueSorterManner {
                                           INCREASING_STRENGTH, INCREASING_STRENGTH_IF_AVAILABLE, DECREASING_STRENGTH, DECREASING_STRENGTH_IF_AVAILABLE
                                        }"""));
    }

    @Test
    void migrate() {
        runTest(
                """
                        changeMoveConfig.withSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        changeMoveConfig.withSorterComparatorClass(Comparator.class);
                        changeMoveConfig.setSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        changeMoveConfig.setSorterComparatorClass(Comparator.class);
                        changeMoveConfig.getSorterWeightFactoryClass();
                        changeMoveConfig.getSorterComparatorClass();
                        swapMoveConfig.withSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        swapMoveConfig.withSorterComparatorClass(Comparator.class);
                        swapMoveConfig.setSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        swapMoveConfig.setSorterComparatorClass(Comparator.class);
                        swapMoveConfig.getSorterWeightFactoryClass();
                        swapMoveConfig.getSorterComparatorClass();
                        entityConfig.withSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        entityConfig.withSorterComparatorClass(Comparator.class);
                        entityConfig.setSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        entityConfig.setSorterComparatorClass(Comparator.class);
                        entityConfig.getSorterWeightFactoryClass();
                        entityConfig.getSorterComparatorClass();
                        entityConfig.setSorterManner(EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE);
                        entityConfig.setSorterManner(EntitySorterManner.DECREASING_DIFFICULTY);
                        valueConfig.withSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        valueConfig.withSorterComparatorClass(Comparator.class);
                        valueConfig.setSorterWeightFactoryClass(SelectionSorterWeightFactory.class);
                        valueConfig.setSorterComparatorClass(Comparator.class);
                        valueConfig.getSorterWeightFactoryClass();
                        valueConfig.getSorterComparatorClass();
                        valueConfig.setSorterManner(ValueSorterManner.INCREASING_STRENGTH);
                        valueConfig.setSorterManner(ValueSorterManner.INCREASING_STRENGTH_IF_AVAILABLE);
                        valueConfig.setSorterManner(ValueSorterManner.DECREASING_STRENGTH);
                        valueConfig.setSorterManner(ValueSorterManner.DECREASING_STRENGTH_IF_AVAILABLE);""",
                """
                        changeMoveConfig.withComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        changeMoveConfig.withComparatorClass(Comparator.class);
                        changeMoveConfig.setComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        changeMoveConfig.setComparatorClass(Comparator.class);
                        changeMoveConfig.getComparatorFactoryClass();
                        changeMoveConfig.getComparatorClass();
                        swapMoveConfig.withComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        swapMoveConfig.withComparatorClass(Comparator.class);
                        swapMoveConfig.setComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        swapMoveConfig.setComparatorClass(Comparator.class);
                        swapMoveConfig.getComparatorFactoryClass();
                        swapMoveConfig.getComparatorClass();
                        entityConfig.withComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        entityConfig.withComparatorClass(Comparator.class);
                        entityConfig.setComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        entityConfig.setComparatorClass(Comparator.class);
                        entityConfig.getComparatorFactoryClass();
                        entityConfig.getComparatorClass();
                        entityConfig.setSorterManner(EntitySorterManner.DESCENDING_IF_AVAILABLE);
                        entityConfig.setSorterManner(EntitySorterManner.DESCENDING);
                        valueConfig.withComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        valueConfig.withComparatorClass(Comparator.class);
                        valueConfig.setComparatorFactoryClass(SelectionSorterWeightFactory.class);
                        valueConfig.setComparatorClass(Comparator.class);
                        valueConfig.getComparatorFactoryClass();
                        valueConfig.getComparatorClass();
                        valueConfig.setSorterManner(ValueSorterManner.ASCENDING);
                        valueConfig.setSorterManner(ValueSorterManner.ASCENDING_IF_AVAILABLE);
                        valueConfig.setSorterManner(ValueSorterManner.DESCENDING);
                        valueConfig.setSorterManner(ValueSorterManner.DESCENDING_IF_AVAILABLE);""");
    }

    private void runTest(String contentBefore, String contentAfter) {
        rewriteRun(java(adjustBefore(contentBefore), adjustAfter(contentAfter)));
    }

    private static String adjustBefore(String content) {
        return """
                import java.util.Comparator;
                import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
                import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
                import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
                import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
                import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;

                @PlanningEntity(difficultyWeightFactoryClass = PlanningEntity.NullDifficultyWeightFactory.class, difficultyComparatorClass = PlanningEntity.NullDifficultyComparator.class)
                public class Test {
                    @PlanningVariable(strengthComparatorClass = PlanningVariable.NullStrengthComparator.class)
                    private Object value;
                    @PlanningVariable(strengthWeightFactoryClass = PlanningVariable.NullStrengthWeightFactory.class)
                    private Object value2;
                    public void validate(ChangeMoveSelectorConfig changeMoveConfig, ListSwapMoveSelectorConfig swapMoveConfig, EntitySelectorConfig entityConfig, ValueSelectorConfig valueConfig) {
                    %8s%s
                    }
                }"""
                .formatted("", content);
    }

    private static String adjustAfter(String content) {
        return """
                import java.util.Comparator;
                import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
                import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
                import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
                import ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullComparator;
                import ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullComparatorFactory;
                import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
                import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
                import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;

                @PlanningEntity(comparatorFactoryClass = PlanningEntity.NullComparatorFactory.class, comparatorClass = PlanningEntity.NullComparator.class)
                public class Test {
                    @PlanningVariable(comparatorClass = NullComparator.class)
                    private Object value;
                    @PlanningVariable(comparatorFactoryClass = NullComparatorFactory.class)
                    private Object value2;
                    public void validate(ChangeMoveSelectorConfig changeMoveConfig, ListSwapMoveSelectorConfig swapMoveConfig, EntitySelectorConfig entityConfig, ValueSelectorConfig valueConfig) {
                    %8s%s
                    }
                }"""
                .formatted("", content);
    }

}
