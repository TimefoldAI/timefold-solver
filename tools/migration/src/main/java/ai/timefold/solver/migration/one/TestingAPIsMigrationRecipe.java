package ai.timefold.solver.migration.one;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.ChangeType;
import org.openrewrite.maven.RemoveDependency;

public class TestingAPIsMigrationRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "Migrate testing APIs to their new packages";
    }

    @Override
    public String getDescription() {
        return getDisplayName() + ".";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangePackage("ai.timefold.solver.test.api.score.stream", "ai.timefold.solver.core.api.score.stream.test",
                        true),
                new ChangePackage("ai.timefold.solver.test.api.solver.change", "ai.timefold.solver.core.api.solver.change",
                        true),
                new ChangeType("ai.timefold.solver.core.preview.api.move.MoveTester",
                        "ai.timefold.solver.core.preview.api.move.test.MoveTester", true),
                new ChangeType("ai.timefold.solver.core.preview.api.move.MoveTestContext",
                        "ai.timefold.solver.core.preview.api.move.test.MoveTestContext", true),
                new ChangeType("ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTester",
                        "ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester", true),
                new ChangeType("ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTestContext",
                        "ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTestContext", true),
                new RemoveDependency("ai.timefold.solver", "timefold-solver-test", null));
    }
}
