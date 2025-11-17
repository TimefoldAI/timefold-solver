package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ConstraintRefRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConstraintRefRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void constraint() {
        runTest("ai.timefold.solver.core.api.score.stream.Constraint",
                "Constraint constraint = null;",
                """
                        String pkg  = constraint.getConstraintPackage();
                        String name = constraint.getConstraintName();
                        String id   = constraint.getConstraintId();""",
                """
                        String pkg  = constraint.getConstraintRef().packageName();
                        String name = constraint.getConstraintRef().constraintName();
                        String id   = constraint.getConstraintRef().constraintId();""");
    }

    @Test
    void constraintMatch() {
        runTest("ai.timefold.solver.core.api.score.constraint.ConstraintMatch",
                "ConstraintMatch constraintMatch = null;",
                """
                        String pkg  = constraintMatch.getConstraintPackage();
                        String name = constraintMatch.getConstraintName();
                        String id   = constraintMatch.getConstraintId();""",
                """
                        String pkg  = constraintMatch.getConstraintRef().packageName();
                        String name = constraintMatch.getConstraintRef().constraintName();
                        String id   = constraintMatch.getConstraintRef().constraintId();""");
    }

    @Test
    void constraintMatchTotal() {
        runTest("ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal",
                "ConstraintMatchTotal constraintMatchTotal = null;",
                """
                        String pkg  = constraintMatchTotal.getConstraintPackage();
                        String name = constraintMatchTotal.getConstraintName();
                        String id   = constraintMatchTotal.getConstraintId();""",
                """
                        String pkg  = constraintMatchTotal.getConstraintRef().packageName();
                        String name = constraintMatchTotal.getConstraintRef().constraintName();
                        String id   = constraintMatchTotal.getConstraintRef().constraintId();""");
    }

    @Test
    void defaultConstraintMatchTotal() {
        runTest("ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal",
                "DefaultConstraintMatchTotal constraintMatchTotal = null;",
                """
                        String pkg  = constraintMatchTotal.getConstraintPackage();
                        String name = constraintMatchTotal.getConstraintName();
                        String id   = constraintMatchTotal.getConstraintId();""",
                """
                        String pkg  = constraintMatchTotal.getConstraintRef().packageName();
                        String name = constraintMatchTotal.getConstraintRef().constraintName();
                        String id   = constraintMatchTotal.getConstraintRef().constraintId();""");
    }

    private void runTest(String implClassFqn, String declaration, @Language("java") String before, @Language("java") String after) {
        rewriteRun(java(wrap(implClassFqn, declaration, before), wrap(implClassFqn, declaration, after)));
    }

    private static @Language("java") String wrap(String implClassFqn, @Language("java") String declaration, @Language("java") String content) {
        return """
                import %s;

                class Test {
                    public static void main(String[] args) {
                        %s
                        %s
                    }
                }""".formatted(implClassFqn, declaration.trim(), content.trim());
    }

}
