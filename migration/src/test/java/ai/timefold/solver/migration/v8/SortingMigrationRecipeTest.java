package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class SortingMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SortingMigrationRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
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
                        changeMoveConfig.withComparatorFactoryClass(ComparatorFactory.class);
                        changeMoveConfig.withComparatorClass(Comparator.class);
                        changeMoveConfig.setComparatorFactoryClass(ComparatorFactory.class);
                        changeMoveConfig.setComparatorClass(Comparator.class);
                        changeMoveConfig.getComparatorFactoryClass();
                        changeMoveConfig.getComparatorClass();
                        swapMoveConfig.withComparatorFactoryClass(ComparatorFactory.class);
                        swapMoveConfig.withComparatorClass(Comparator.class);
                        swapMoveConfig.setComparatorFactoryClass(ComparatorFactory.class);
                        swapMoveConfig.setComparatorClass(Comparator.class);
                        swapMoveConfig.getComparatorFactoryClass();
                        swapMoveConfig.getComparatorClass();
                        entityConfig.withComparatorFactoryClass(ComparatorFactory.class);
                        entityConfig.withComparatorClass(Comparator.class);
                        entityConfig.setComparatorFactoryClass(ComparatorFactory.class);
                        entityConfig.setComparatorClass(Comparator.class);
                        entityConfig.getComparatorFactoryClass();
                        entityConfig.getComparatorClass();
                        entityConfig.setSorterManner(EntitySorterManner.DESCENDING_IF_AVAILABLE);
                        entityConfig.setSorterManner(EntitySorterManner.DESCENDING);
                        valueConfig.withComparatorFactoryClass(ComparatorFactory.class);
                        valueConfig.withComparatorClass(Comparator.class);
                        valueConfig.setComparatorFactoryClass(ComparatorFactory.class);
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

                import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
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
