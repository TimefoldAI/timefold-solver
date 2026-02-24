package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class EnvironmentMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EnvironmentMigrationRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void migrate() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.config.solver.EnvironmentMode;

                        public class Test {
                                EnvironmentMode fast = EnvironmentMode.FAST_ASSERT;
                                EnvironmentMode reproducible = EnvironmentMode.REPRODUCIBLE;
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.config.solver.EnvironmentMode;

                        public class Test {
                                EnvironmentMode fast = EnvironmentMode.STEP_ASSERT;
                                EnvironmentMode reproducible = EnvironmentMode.NO_ASSERT;
                        }"""));
    }

}
