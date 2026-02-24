package ai.timefold.solver.migration.v8;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ReplaceConstantWithAnotherConstant;

public class EnvironmentMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Use non-deprecated environment constants";
    }

    @Override
    public String getDescription() {
        return "Use non-deprecated environment constants.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ReplaceConstantWithAnotherConstant("ai.timefold.solver.core.config.solver.EnvironmentMode.FAST_ASSERT",
                        "ai.timefold.solver.core.config.solver.EnvironmentMode.STEP_ASSERT"),
                new ReplaceConstantWithAnotherConstant("ai.timefold.solver.core.config.solver.EnvironmentMode.REPRODUCIBLE",
                        "ai.timefold.solver.core.config.solver.EnvironmentMode.NO_ASSERT"));
    }
}
