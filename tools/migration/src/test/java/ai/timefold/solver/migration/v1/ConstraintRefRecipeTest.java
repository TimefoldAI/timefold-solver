package ai.timefold.solver.migration.v1;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class ConstraintRefRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConstraintRefRecipe())
                .afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score.stream;

                                        public interface Constraint {
                                            String getConstraintPackage();
                                            String getConstraintName();
                                            String getConstraintId();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.constraint;

                                        public interface ConstraintMatch {
                                            String getConstraintPackage();
                                            String getConstraintName();
                                            String getConstraintId();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.constraint;

                                        public interface ConstraintMatchTotal {
                                            String getConstraintPackage();
                                            String getConstraintName();
                                            String getConstraintId();
                                        }""",
                                """
                                        package ai.timefold.solver.core.impl.score.constraint;
                                        import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;

                                        public interface DefaultConstraintMatchTotal extends ConstraintMatchTotal {
                                        }"""));
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

    private void runTest(String implClassFqn, String declaration, String before, String after) {
        rewriteRun(java(wrap(implClassFqn, declaration, before), wrap(implClassFqn, declaration, after)));
    }

    private static String wrap(String implClassFqn, String declaration, String content) {
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
