package ai.timefold.solver.migration.one;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.maven.RemoveDependency;

public class PersistenceCommonMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Migrate away from timefold-solver-persistence-common";
    }

    @Override
    public String getDescription() {
        return getDisplayName() + ".";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangePackage("ai.timefold.solver.persistence.common.api.domain.solution",
                        "ai.timefold.solver.core.api.domain.solution",
                        true),
                new RemoveDependency("ai.timefold.solver", "timefold-solver-persistence-common", null));
    }
}
