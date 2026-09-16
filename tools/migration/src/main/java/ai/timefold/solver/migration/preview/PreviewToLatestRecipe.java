package ai.timefold.solver.migration.preview;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;

public final class PreviewToLatestRecipe extends AbstractRecipe {

    @Override
    public String getName() {
        return "ai.timefold.solver.migration.PreviewToLatest";
    }

    @Override
    public String getDisplayName() {
        return "Upgrade to the latest Timefold Solver preview APIs";
    }

    @Override
    public String getDescription() {
        return "Replace all your calls to renamed/removed preview API types and methods of Timefold Solver with their proper alternatives.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new NeighborhoodsMigrationRecipe());
    }

}
