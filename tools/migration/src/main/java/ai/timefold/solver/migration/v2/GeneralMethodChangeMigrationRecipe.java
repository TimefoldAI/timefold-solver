package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;

public class GeneralMethodChangeMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate legacy methods to the new structure";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy methods to the new structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Planning Id
                new ChangeMethodName(
                        "ai.timefold.solver.benchmark.api.PlannerBenchmark benchmarkAndShowReportInBrowser()",
                        "benchmark", true, false));

    }
}
