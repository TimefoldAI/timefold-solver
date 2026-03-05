package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ProblemIdDeletionMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ProblemIdDeletionMigrationRecipe())
                //                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.solver;
                                        import ai.timefold.solver.core.api.solver.SolverBuilder;
                                        import ai.timefold.solver.core.api.solver.SolverJob;
                                        public interface SolverManager<Solution_, ProblemId_> {
                                           SolverManager<Solution_, ProblemId_> create(Object param);
                                           SolverJobBuilder<Solution_, ProblemId_> solveBuilder();
                                           SolverJob<Solution_, ProblemId_> solve(Object parameter, Object secondParameter);
                                           SolverJob<Solution_, ProblemId_> solveAndListen(Object parameter, Object secondParameter);
                                        }""",
                                        """
                                        package ai.timefold.solver.core.api.solver;
                                        public interface SolverJobBuilder<Solution_, ProblemId_> {
                                        }""",
                                        """
                                        package ai.timefold.solver.core.api.solver;
                                        public interface SolverJob<Solution_, ProblemId_> {
                                        }"""));
    }

    @Test
    void removeConstraintStreamImplType() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.solver.SolverManager;
                        import ai.timefold.solver.core.api.solver.SolverJobBuilder;
                        import ai.timefold.solver.core.api.solver.SolverJob;

                        public class Test<Solution_, ProblemId_> {
                                SolverManager<Solution_, ProblemId_> solverManager;
                                SolverJobBuilder<Solution_, ProblemId_> solverJobBuilder;
                                SolverJob<Solution_, ProblemId_> solverJob;
                                public void test() {
                                    SolverManager<Solution_, ProblemId_> result1 = solverManager.<Solution_, ProblemId_>create(null);
                                    SolverJobBuilder<Solution_, ProblemId_> result2 = solverManager.<Solution_, ProblemId_>solveBuilder();
                                    SolverJob<Solution_, ProblemId_> result1 = solverManager.<Solution_, ProblemId_>solve(null, null);
                                    SolverJob<Solution_, ProblemId_> result1 = solverManager.<Solution_, ProblemId_>solveAndListen(null, null);
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.solver.SolverManager;
                        import ai.timefold.solver.core.api.solver.SolverJobBuilder;
                        import ai.timefold.solver.core.api.solver.SolverJob;

                        public class Test<Solution_, ProblemId_> {
                                SolverManager<Solution_> solverManager;
                                SolverJobBuilder<Solution_> solverJobBuilder;
                                SolverJob<Solution_> solverJob;
                                public void test() {
                                    SolverManager<Solution_> result1 = solverManager.<Solution_>create(null);
                                    SolverJobBuilder<Solution_> result2 = solverManager.<Solution_>solveBuilder();
                                    SolverJob<Solution_> result1 = solverManager.<Solution_>solve(null, null);
                                    SolverJob<Solution_> result1 = solverManager.<Solution_>solveAndListen(null, null);
                                }
                        }"""));
    }

}
