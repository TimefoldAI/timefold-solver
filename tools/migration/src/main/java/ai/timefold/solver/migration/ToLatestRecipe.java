package ai.timefold.solver.migration;

import java.util.List;

import ai.timefold.solver.migration.preview.PreviewToLatestRecipe;
import ai.timefold.solver.migration.v1.ToLatestV1Recipe;
import ai.timefold.solver.migration.v2.ToLatestV2Recipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveUnusedImports;

public final class ToLatestRecipe extends AbstractRecipe {

    @Override
    public String getName() {
        return "ai.timefold.solver.migration.ToLatest";
    }

    @Override
    public String getDisplayName() {
        return "Upgrade to the latest Timefold Solver";
    }

    @Override
    public String getDescription() {
        return "Replace all your calls to deleted/deprecated types and methods of Timefold Solver with their proper alternatives.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ToLatestV1Recipe(),
                new ToLatestV2Recipe(),
                new PreviewToLatestRecipe(),
                new RemoveUnusedImports());
    }

}
