package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipeTest;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;

class ConstraintArgRemovalMigrationRecipeTest extends AbstractRecipeTest {

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
