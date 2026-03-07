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
class RemoveConstraintPackageRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveConstraintPackageRecipe())
                .afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score;

                                        public interface Score {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;

                                        public interface ConstraintBuilder {
                                            Object asConstraint(String parameter, String secondParameter)
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

                                        public interface ConstraintStream extends ConstraintBuilder {
                                            ConstraintStream penalize(Score score);
                                            BiConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.uni;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;

                                        public interface UniConstraintStream extends ConstraintStream {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.bi;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;

                                        public interface BiConstraintStream extends ConstraintStream {
                                            TriConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.tri;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;

                                        public interface TriConstraintStream extends ConstraintStream {
                                            QuadConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.quad;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;

                                        public interface QuadConstraintStream extends ConstraintStream {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;

                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

                                        public interface ConstraintFactory {
                                            UniConstraintStream forEach(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.buildin.hardsoft;
                                        import ai.timefold.solver.core.api.score.Score;

                                        public interface HardSoftScore {
                                            public Score ONE_HARD;
                                        }"""));
    }

    @Test
    void uni() {
        rewriteRun(
                java(
                        wrap("""
                                        return f.forEach(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package", "My constraint");\
                                """),
                        wrap("""
                                        return f.forEach(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package.My constraint");\
                                """)));
    }

    @Test
    void bi() {
        rewriteRun(
                java(
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package", "My constraint");\
                                """),
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package.My constraint");\
                                """)));
    }

    @Test
    void tri() {
        rewriteRun(
                java(
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package", "My constraint");\
                                """),
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package.My constraint");\
                                """)));
    }

    @Test
    void quad() {
        rewriteRun(
                java(
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package", "My constraint");\
                                """),
                        wrap("""
                                        return f.forEach(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .join(String.class)
                                                .penalize(HardSoftScore.ONE_HARD)
                                                .asConstraint("My package.My constraint");\
                                """)));
    }

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static String wrap(String content) {
        return "import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;\n" +
                "import ai.timefold.solver.core.api.score.stream.ConstraintFactory;\n" +
                "import ai.timefold.solver.core.api.score.stream.Constraint;\n" +
                "\n" +
                "class Test {\n" +
                "    Object myConstraint(ConstraintFactory f) {\n" +
                content + "\n" +
                "    }" +
                "}\n";
    }

}
