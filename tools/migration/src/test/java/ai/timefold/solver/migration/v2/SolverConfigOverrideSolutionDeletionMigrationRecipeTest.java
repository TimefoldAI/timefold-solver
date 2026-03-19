package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class SolverConfigOverrideSolutionDeletionMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new SolverConfigOverrideSolutionDeletionMigrationRecipe())
                //                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.solver;
                                        public interface SolverConfigOverride<Solution_> {
                                        }"""));
    }

    @Test
    void removeGenericType() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.solver.SolverConfigOverride;

                        public class Test<Solution_, ProblemId_> {

                                SolverConfigOverride<Solution_> override = new SolverConfigOverride<>();

                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.solver.SolverConfigOverride;

                        public class Test<Solution_, ProblemId_> {

                                SolverConfigOverride override = new SolverConfigOverride();

                        }"""));
    }

}
