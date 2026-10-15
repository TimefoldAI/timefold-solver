package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipeTest;
import ai.timefold.solver.migration.NoWildCardImportStyle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.TypeValidation;

class TestingAPIsMigrationRecipeTest extends AbstractRecipeTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new TestingAPIsMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion().styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn("package ai.timefold.solver.test.api.score.stream; public class ConstraintVerifier {}",
                                "package ai.timefold.solver.test.api.solver.change; public class MockProblemChangeDirector {}",
                                "package ai.timefold.solver.core.preview.api.move; public class MoveTester {}",
                                "package ai.timefold.solver.core.preview.api.neighborhood; public class NeighborhoodTester {}"));
    }

    @Test
    void migrate() {
        rewriteRun(java("""
                package timefold;

                import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
                import ai.timefold.solver.test.api.solver.change.MockProblemChangeDirector;
                import ai.timefold.solver.core.preview.api.move.MoveTester;
                import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTester;

                public class Test {
                        ConstraintVerifier constraintVerifier;
                        MockProblemChangeDirector problemChangeDirector;
                        MoveTester moveTester;
                        NeighborhoodTester neighborhoodTester;
                }""", """
                package timefold;

                import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
                import ai.timefold.solver.core.api.solver.change.MockProblemChangeDirector;
                import ai.timefold.solver.core.preview.api.move.test.MoveTester;
                import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;

                public class Test {
                        ConstraintVerifier constraintVerifier;
                        MockProblemChangeDirector problemChangeDirector;
                        MoveTester moveTester;
                        NeighborhoodTester neighborhoodTester;
                }"""));
    }

}
