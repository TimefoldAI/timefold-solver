package ai.timefold.solver.migration.v8;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeAnnotationAttributeName;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.ReplaceConstantWithAnotherConstant;

public class SortingMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Use non-deprecated related sorting fields and methods";
    }

    @Override
    public String getDescription() {
        return "Use non-deprecated related sorting fields and methods.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Update ComparatorFactory
                new ChangeMethodName(
                        "ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory createSorterWeight(..)",
                        "createComparator", true, null),
                new ChangeType("ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory",
                        "ai.timefold.solver.core.api.domain.common.ComparatorFactory", true),
                // Update PlanningVariable sorting fields
                new ChangeAnnotationAttributeName("ai.timefold.solver.core.api.domain.variable.PlanningVariable",
                        "strengthComparatorClass", "comparatorClass"),
                new ChangeAnnotationAttributeName("ai.timefold.solver.core.api.domain.variable.PlanningVariable",
                        "strengthWeightFactoryClass", "comparatorFactoryClass"),
                new ChangeType("ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullStrengthComparator",
                        "ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullComparator", true),
                new ChangeType("ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullStrengthWeightFactory",
                        "ai.timefold.solver.core.api.domain.variable.PlanningVariable.NullComparatorFactory", true),
                // Update PlanningEntity sorting fields
                new ChangeAnnotationAttributeName("ai.timefold.solver.core.api.domain.entity.PlanningEntity",
                        "difficultyComparatorClass", "comparatorClass"),
                new ChangeAnnotationAttributeName("ai.timefold.solver.core.api.domain.entity.PlanningEntity",
                        "difficultyWeightFactoryClass", "comparatorFactoryClass"),
                new ChangeType("ai.timefold.solver.core.api.domain.entity.PlanningEntity.NullDifficultyComparator",
                        "ai.timefold.solver.core.api.domain.entity.PlanningEntity.NullComparator", true),
                new ChangeType("ai.timefold.solver.core.api.domain.entity.PlanningEntity.NullDifficultyWeightFactory",
                        "ai.timefold.solver.core.api.domain.entity.PlanningEntity.NullComparatorFactory", true),
                // Update MoveSelectorConfig sorting methods
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig getSorterComparatorClass(..)",
                        "getComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig setSorterComparatorClass(..)",
                        "setComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig withSorterComparatorClass(..)",
                        "withComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig getSorterWeightFactoryClass(..)",
                        "getComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig setSorterWeightFactoryClass(..)",
                        "setComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig withSorterWeightFactoryClass(..)",
                        "withComparatorFactoryClass", true, null),
                // Update EntitySelectorConfig sorting methods
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig getSorterComparatorClass(..)",
                        "getComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig setSorterComparatorClass(..)",
                        "setComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig withSorterComparatorClass(..)",
                        "withComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig getSorterWeightFactoryClass(..)",
                        "getComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig setSorterWeightFactoryClass(..)",
                        "setComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig withSorterWeightFactoryClass(..)",
                        "withComparatorFactoryClass", true, null),
                // Update ValueSelectorConfig sorting methods
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig getSorterComparatorClass(..)",
                        "getComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig setSorterComparatorClass(..)",
                        "setComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig withSorterComparatorClass(..)",
                        "withComparatorClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig getSorterWeightFactoryClass(..)",
                        "getComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig setSorterWeightFactoryClass(..)",
                        "setComparatorFactoryClass", true, null),
                new ChangeMethodName(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig withSorterWeightFactoryClass(..)",
                        "withComparatorFactoryClass", true, null),
                // Update EntitySorterManner
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY",
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DESCENDING"),
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE",
                        "ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DESCENDING_IF_AVAILABLE"),
                // Update ValueSorterManner
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.DECREASING_STRENGTH",
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.DESCENDING"),
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.DECREASING_STRENGTH_IF_AVAILABLE",
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.DESCENDING_IF_AVAILABLE"),
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.INCREASING_STRENGTH",
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.ASCENDING"),
                new ReplaceConstantWithAnotherConstant(
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.INCREASING_STRENGTH_IF_AVAILABLE",
                        "ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.ASCENDING_IF_AVAILABLE"));
    }
}
