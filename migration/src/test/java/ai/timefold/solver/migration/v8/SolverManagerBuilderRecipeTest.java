package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class SolverManagerBuilderRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SolverManagerBuilderRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void solverMethods() {
        runTest(
                """
                        solverManager.solve(1L, problem, finalSolutionConsumer, exceptionHandler);
                        solverManager.solve(1L, id -> problem, finalSolutionConsumer);
                        solverManager.solve(1L, id -> problem, finalSolutionConsumer, exceptionHandler);
                        solverManager.solveAndListen(1L, id -> problem, bestSolutionConsumer);
                        solverManager.solveAndListen(1L, id -> problem, bestSolutionConsumer, exceptionHandler);
                        solverManager.solveAndListen(1L, id -> problem, bestSolutionConsumer, finalSolutionConsumer, exceptionHandler);
                        solverManagerWithGenericType.solve(1L, problem, finalSolutionConsumer, exceptionHandler);
                        solverManagerWithGenericType.solve(1L, id -> problem, finalSolutionConsumer);
                        solverManagerWithGenericType.solve(1L, id -> problem, finalSolutionConsumer, exceptionHandler);
                        solverManagerWithGenericType.solveAndListen(1L, id -> problem, bestSolutionConsumer);
                        solverManagerWithGenericType.solveAndListen(1L, id -> problem, bestSolutionConsumer, exceptionHandler);
                        solverManagerWithGenericType.solveAndListen(1L, id -> problem, bestSolutionConsumer, finalSolutionConsumer, exceptionHandler);
                        """,
                """
                        solverManager.solveBuilder().withProblemId(1L).withProblem(problem).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManager.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withFinalBestSolutionConsumer(finalSolutionConsumer).run();
                        solverManager.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManager.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).run();
                        solverManager.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManager.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblem(problem).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withFinalBestSolutionConsumer(finalSolutionConsumer).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        solverManagerWithGenericType.solveBuilder().withProblemId(1L).withProblemFinder(id -> problem).withBestSolutionConsumer(bestSolutionConsumer).withFinalBestSolutionConsumer(finalSolutionConsumer).withExceptionHandler(exceptionHandler).run();
                        """);
    }

    private void runTest(String before, String after) {
        rewriteRun(java(wrap(before), wrap(after)));
    }

    private static String wrap(String content) {
        return """
                import java.util.function.BiConsumer;
                import java.util.function.Consumer;
                import java.util.function.Function;
                import ai.timefold.solver.core.api.solver.SolverFactory;
                import ai.timefold.solver.core.api.solver.SolverManager;

                class test {
                  public static void main(String[] args) {
                    SolverFactory solverFactory = SolverFactory.create(null);
                    SolverManager solverManager = solverFactory.buildSolver();
                    SolverManager<Object, Long> solverManagerWithGenericType = solverFactory.buildSolver();
                    Object problem = new Object();
                    Consumer finalSolutionConsumer = () -> {};
                    Consumer bestSolutionConsumer = () -> {};
                    BiConsumer exceptionHandler = (problemId, throwable) -> {};
                %s
                  }
                }
                """
                .formatted(content.indent(6).replaceAll("\n$", ""));
    }

}
