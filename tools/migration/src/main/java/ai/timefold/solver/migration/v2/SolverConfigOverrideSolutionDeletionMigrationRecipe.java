package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.common.RemoveGenericTypeRecipe;

import org.openrewrite.Recipe;

public class SolverConfigOverrideSolutionDeletionMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Remove the Solution_ generic type from SolverConfigOverride";
    }

    @Override
    public String getDescription() {
        return getDisplayName() + ".";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new RemoveGenericTypeRecipe("ai.timefold.solver.core.api.solver.SolverConfigOverride", 0));
    }
}
