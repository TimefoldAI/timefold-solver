package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ConstraintArgRemovalMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ConstraintArgRemovalMigrationRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score.constraint;
                                        public interface ConstraintRef {
                                            static ConstraintRef of(String constraintPackage, String constraintName) {
                                                return null;
                                            }
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        public interface ConstraintBuilder {
                                            Object asConstraint(String constraintPackage, String constraintName);
                                        }"""));
    }

    @Test
    void constraintRefOf() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

                        class Test {
                            void method() {
                                ConstraintRef ref = ConstraintRef.of("com.example", "myConstraint");
                            }
                        }""",
                """
                        import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

                        class Test {
                            void method() {
                                ConstraintRef ref = ConstraintRef.of("myConstraint");
                            }
                        }"""));
    }

    @Test
    void constraintBuilderAsConstraint() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

                        class Test {
                            void method(ConstraintBuilder builder) {
                                builder.asConstraint("com.example", "myConstraint");
                            }
                        }""",
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

                        class Test {
                            void method(ConstraintBuilder builder) {
                                builder.asConstraint("myConstraint");
                            }
                        }"""));
    }

}
