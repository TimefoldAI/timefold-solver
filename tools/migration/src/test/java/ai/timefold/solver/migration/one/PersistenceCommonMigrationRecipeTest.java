package ai.timefold.solver.migration.one;

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

import static org.openrewrite.java.Assertions.java;

@Execution(ExecutionMode.CONCURRENT)
class PersistenceCommonMigrationRecipeTest implements RewriteTest {

    private static final class NoWildCardImportStyle extends NamedStyles {

        public NoWildCardImportStyle() {
            super(UUID.randomUUID(), "ImportStyle", "ImportStyle", "ImportStyle", Collections.emptySet(),
                    List.of(ImportLayoutStyle.builder().classCountToUseStarImport(9999999).importStaticAllOthers()
                            .importAllOthers().build()));
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new PersistenceCommonMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion().styles(List.of(new NoWildCardImportStyle()))
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                "package ai.timefold.solver.persistence.common.api.domain.solution; public class SolutionFileIO {}"));
    }

    @Test
    void migrate() {
        rewriteRun(java("""
                package timefold;

                import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

                public class Test {
                        SolutionFileIO solutionFileIO;
                }""", """
                package timefold;

                import ai.timefold.solver.core.api.domain.solution.SolutionFileIO;

                public class Test {
                        SolutionFileIO solutionFileIO;
                }"""));
    }

}
