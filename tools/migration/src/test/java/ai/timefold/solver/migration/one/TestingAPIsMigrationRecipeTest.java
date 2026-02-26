package ai.timefold.solver.migration.one;

import static org.openrewrite.java.Assertions.java;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class TestingAPIsMigrationRecipeTest implements RewriteTest {

    private static final class NoWildCardImportStyle extends NamedStyles {

        public NoWildCardImportStyle() {
            super(UUID.randomUUID(), "ImportStyle", "ImportStyle", "ImportStyle", Collections.emptySet(),
                    List.of(ImportLayoutStyle.builder().classCountToUseStarImport(9999999).importStaticAllOthers()
                            .importAllOthers().build()));
        }
    }

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
