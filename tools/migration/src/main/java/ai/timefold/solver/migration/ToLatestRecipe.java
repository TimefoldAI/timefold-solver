package ai.timefold.solver.migration;

import java.util.List;

import ai.timefold.solver.migration.v2.ConstraintArgRemovalMigrationRecipe;
import ai.timefold.solver.migration.v2.ConstraintMetadataMigrationRecipe;
import ai.timefold.solver.migration.v2.GeneralDependencyDeleteMigrationRecipe;
import ai.timefold.solver.migration.v2.GeneralMethodChangeNameMigrationRecipe;
import ai.timefold.solver.migration.v2.GeneralMethodDeleteInvocationMigrationRecipe;
import ai.timefold.solver.migration.v2.GeneralPackageRenameMigrationRecipe;
import ai.timefold.solver.migration.v2.GeneralTypeChangeMigrationRecipe;
import ai.timefold.solver.migration.v2.PlanningSolutionAnnotationCleanupMigrationRecipe;
import ai.timefold.solver.migration.v2.ProblemIdDeletionMigrationRecipe;
import ai.timefold.solver.migration.v2.SolverConfigOverrideSolutionDeletionMigrationRecipe;
import ai.timefold.solver.migration.v2.TestingAPIsMigrationRecipe;

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
