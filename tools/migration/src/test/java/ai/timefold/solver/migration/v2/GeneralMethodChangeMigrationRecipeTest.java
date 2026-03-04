package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class GeneralMethodChangeMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralMethodChangeMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.benchmark.api; public class PlannerBenchmark {
                                            void benchmarkAndShowReportInBrowser();
                                        }"""));
    }

    @Test
    void migrate() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.benchmark.api.PlannerBenchmark;

                        public class Test {
                                PlannerBenchmark benchmark;
                                public void test() {
                                    benchmark.benchmarkAndShowReportInBrowser();
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.benchmark.api.PlannerBenchmark;

                        public class Test {
                                PlannerBenchmark benchmark;
                                public void test() {
                                    benchmark.benchmark();
                                }
                        }"""));
    }

}
