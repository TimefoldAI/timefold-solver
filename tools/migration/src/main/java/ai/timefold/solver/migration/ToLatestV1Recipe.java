package ai.timefold.solver.migration;

import java.util.List;

import ai.timefold.solver.migration.v1.AsConstraintRecipe;
import ai.timefold.solver.migration.v1.ConstraintRefRecipe;
import ai.timefold.solver.migration.v1.NullableRecipe;
import ai.timefold.solver.migration.v1.RemoveConstraintPackageRecipe;
import ai.timefold.solver.migration.v1.ScoreGettersRecipe;
import ai.timefold.solver.migration.v1.ScoreManagerMethodsRecipe;
import ai.timefold.solver.migration.v1.SingleConstraintAssertionMethodsRecipe;
import ai.timefold.solver.migration.v1.SolutionManagerRecommendAssignmentRecipe;
import ai.timefold.solver.migration.v1.SolverManagerBuilderRecipe;
import ai.timefold.solver.migration.v1.SortingMigrationRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.properties.ChangePropertyKey;

public final class ToLatestV1Recipe extends AbstractRecipe {

    @Override
    public String getName() {
        return "ai.timefold.solver.migration.ToLatestV1";
    }

    @Override
    public String getDisplayName() {
        return "Upgrade to the latest Timefold Solver 1.x";
    }

    @Override
    public String getDescription() {
        return "Replace all your calls to deleted/deprecated types and methods of Timefold Solver with their proper alternatives.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangePropertyKey("timefold.solver.solve-length", "timefold.solver.solve.duration", null, null),
                new ChangePropertyKey("quarkus.timefold.solver.solve-length", "quarkus.timefold.solver.solve.duration", null,
                        null),
                new ChangeMethodName("ai.timefold.solver.core.api.score.stream.ConstraintFactory from(Class)",
                        "forEach", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintFactory fromUnfiltered(Class)",
                        "forEachIncludingUnassigned", true, false),
                new ChangeMethodName(
                        "ai.timefold.solver.core.api.score.stream.ConstraintFactory fromUniquePair(..)",
                        "forEachUniquePair", true, false),
                new ScoreManagerMethodsRecipe(),
                new ChangeType("ai.timefold.solver.core.api.score.ScoreManager",
                        "ai.timefold.solver.core.api.solver.SolutionManager", true),
                new ScoreGettersRecipe(),
                new ConstraintRefRecipe(),
                new SolverManagerBuilderRecipe(),
                new NullableRecipe(),
                new SingleConstraintAssertionMethodsRecipe(),
                new AsConstraintRecipe(),
                new RemoveConstraintPackageRecipe(),
                new SolutionManagerRecommendAssignmentRecipe(),
                new SortingMigrationRecipe());
    }

}