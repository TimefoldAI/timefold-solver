package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.ChangeVersionRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveUnusedImports;

public final class ToLatestV2Recipe extends AbstractRecipe {

    @Override
    public String getName() {
        return "ai.timefold.solver.migration.ToLatestV2";
    }

    @Override
    public String getDisplayName() {
        return "Upgrade to the latest Timefold Solver 2.x";
    }

    @Override
    public String getDescription() {
        return "Replace all your calls to deleted/deprecated types and methods of Timefold Solver with their proper alternatives.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeVersionRecipe(),
                new ConstraintArgRemovalMigrationRecipe(),
                new ConstraintMetadataMigrationRecipe(),
                new PlanningSolutionAnnotationCleanupMigrationRecipe(),
                new GeneralMethodDeleteInvocationMigrationRecipe(),
                new GeneralMethodChangeNameMigrationRecipe(),
                new GeneralTypeChangeMigrationRecipe(),
                new ProblemIdDeletionMigrationRecipe(),
                new TestingAPIsMigrationRecipe(),
                new GeneralDependencyDeleteMigrationRecipe(),
                new GeneralPackageRenameMigrationRecipe(),
                new SolverConfigOverrideSolutionDeletionMigrationRecipe(),
                new RemoveUnusedImports());
    }

}
