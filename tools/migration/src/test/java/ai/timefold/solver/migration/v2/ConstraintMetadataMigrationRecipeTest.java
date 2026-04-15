package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ConstraintMetadataMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ConstraintMetadataMigrationRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        public interface ConstraintBuilder {
                                            Object asConstraintDescribed(String name, String description);
                                            Object asConstraintDescribed(String name, String group, String description);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        public interface ConstraintRef {
                                            String constraintName();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.analysis;
                                        public interface ConstraintAnalysis<Score_> {
                                            String constraintName();
                                        }"""));
    }

    @Test
    void asConstraintDescribedTwoArgs() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

                        class Test {
                            void method(ConstraintBuilder builder) {
                                builder.asConstraintDescribed("myConstraint", "Some description.");
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

    @Test
    void asConstraintDescribedThreeArgs() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

                        class Test {
                            void method(ConstraintBuilder builder) {
                                builder.asConstraintDescribed("myConstraint", "myGroup", "Some description.");
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

    @Test
    void constraintRefConstraintName() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintRef;

                        class Test {
                            void method(ConstraintRef ref) {
                                String name = ref.constraintName();
                            }
                        }""",
                """
                        import ai.timefold.solver.core.api.score.stream.ConstraintRef;

                        class Test {
                            void method(ConstraintRef ref) {
                                String name = ref.id();
                            }
                        }"""));
    }

    @Test
    void constraintAnalysisConstraintName() {
        rewriteRun(java(
                """
                        import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;

                        class Test {
                            void method(ConstraintAnalysis<?> analysis) {
                                String name = analysis.constraintName();
                            }
                        }""",
                """
                        import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;

                        class Test {
                            void method(ConstraintAnalysis<?> analysis) {
                                String name = analysis.constraintId();
                            }
                        }"""));
    }

}
