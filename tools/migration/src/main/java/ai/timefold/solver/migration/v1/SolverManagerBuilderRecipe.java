package ai.timefold.solver.migration.v1;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import ai.timefold.solver.migration.common.CustomChangeMethodRecipe;
import org.openrewrite.Recipe;

public final class SolverManagerBuilderRecipe extends AbstractRecipe {

    @Override
    public String getDisplayName() {
        return "SolverManager: use builder API";
    }

    @Override
    public String getDescription() {
        return "Use `solveBuilder()` instead of deprecated solve methods on `SolveManager`.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solve(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.BiConsumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solve(*..*,*..*,java.util.function.Consumer,java.util.function.BiConsumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblem(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solve(*..*,java.util.function.Function,java.util.function.Consumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).run()"),
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).run()"),
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.BiConsumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"),
                new CustomChangeMethodRecipe("ai.timefold.solver.core.api.solver.SolverManager",
                        "solveAndListen(*..*,java.util.function.Function,java.util.function.Consumer,java.util.function.Consumer,java.util.function.BiConsumer)",
                        ".solveBuilder().withProblemId(#{any()}).withProblemFinder(#{any()}).withBestSolutionConsumer(#{any(java.util.function.Consumer)}).withFinalBestSolutionConsumer(#{any(java.util.function.Consumer)}).withExceptionHandler(#{any(java.util.function.BiConsumer)}).run()"));
    }

}
