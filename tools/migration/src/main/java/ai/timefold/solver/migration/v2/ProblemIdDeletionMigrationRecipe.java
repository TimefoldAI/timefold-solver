package ai.timefold.solver.migration.v2;

import ai.timefold.solver.migration.AbstractRecipe;
import ai.timefold.solver.migration.common.RemoveGenericTypeFromMethodRecipe;
import ai.timefold.solver.migration.common.RemoveGenericTypeRecipe;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;

import java.util.List;

public class ProblemIdDeletionMigrationRecipe extends AbstractRecipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Remove the ProblemId generic type";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Remove the ProblemId generic type.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Persistence common
                new RemoveGenericTypeRecipe("ai.timefold.solver.core.api.solver.SolverManager", 1),
                new RemoveGenericTypeFromMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager create(..)", 1),
                new RemoveGenericTypeFromMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager solveBuilder()", 1),
                new RemoveGenericTypeFromMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager solve(..)", 1),
                new RemoveGenericTypeFromMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager solveAndListen(..)", 1),
                new RemoveGenericTypeRecipe("ai.timefold.solver.core.api.solver.SolverJobBuilder", 1),
                new RemoveGenericTypeRecipe("ai.timefold.solver.core.api.solver.SolverJob", 1));
    }
}

