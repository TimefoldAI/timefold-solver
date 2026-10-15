package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipeTest;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;

class PlanningSolutionAnnotationCleanupMigrationRecipeTest extends AbstractRecipeTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new PlanningSolutionAnnotationCleanupMigrationRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.domain.solution;
                                        public @interface PlanningSolution {
                                            String lookUpStrategyType() default "";
                                            String autoDiscoverMemberType() default "";
                                        }"""));
    }

    @Test
    void removesLookUpStrategyType() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution(lookUpStrategyType = "PLANNING_ID")
                        class MySolution {
                        }""",
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution
                        class MySolution {
                        }"""));
    }

    @Test
    void removesAutoDiscoverMemberType() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution(autoDiscoverMemberType = "FIELD")
                        class MySolution {
                        }""",
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution
                        class MySolution {
                        }"""));
    }

    @Test
    void removesBothAttributes() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution(lookUpStrategyType = "PLANNING_ID", autoDiscoverMemberType = "FIELD")
                        class MySolution {
                        }""",
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution
                        class MySolution {
                        }"""));
    }

    @Test
    void unrelatedAnnotationUntouched() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

                        @PlanningSolution
                        class MySolution {
                        }"""));
    }

}
