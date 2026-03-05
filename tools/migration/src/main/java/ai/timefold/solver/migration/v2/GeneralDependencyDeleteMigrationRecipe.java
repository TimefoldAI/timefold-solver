package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.maven.RemoveDependency;

public class GeneralDependencyDeleteMigrationRecipe extends AbstractRecipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Remove dependencies that no longer exist";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Remove dependencies that no longer exist.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new RemoveDependency("ai.timefold.solver", "timefold-solver-persistence-common", null),
                new RemoveDependency("ai.timefold.solver", "timefold-solver-webui", null),
                new RemoveDependency("ai.timefold.solver", "timefold-solver-jsonb", null));
    }
}
