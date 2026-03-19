package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveAnnotationAttribute;

public final class PlanningSolutionAnnotationCleanupMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Remove deprecated attributes from @PlanningSolution";
    }

    @Override
    public String getDescription() {
        return "Removes deprecated lookUpStrategyType and autoDiscoverMemberType attributes from @PlanningSolution.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new RemoveAnnotationAttribute("ai.timefold.solver.core.api.domain.solution.PlanningSolution",
                        "lookUpStrategyType"),
                new RemoveAnnotationAttribute("ai.timefold.solver.core.api.domain.solution.PlanningSolution",
                        "autoDiscoverMemberType"));
    }

}