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
class NullableRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NullableRecipe())
                .afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;

                                        public interface ConstraintStream {
                                            BiConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.uni;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;

                                        public interface UniConstraintStream extends ConstraintStream {
                                            void ifExistsIncludingNullVars(Class parameter);
                                            void ifExistsOtherIncludingNullVars(Class parameter);
                                            void ifNotExistsIncludingNullVars(Class parameter);
                                            void ifNotExistsOtherIncludingNullVars(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.bi;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;

                                        public interface BiConstraintStream extends ConstraintStream {
                                            void ifExistsIncludingNullVars(Class parameter);
                                            void ifNotExistsIncludingNullVars(Class parameter);

                                            TriConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.tri;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;

                                        public interface TriConstraintStream extends ConstraintStream {
                                            void ifExistsIncludingNullVars(Class parameter);
                                            void ifNotExistsIncludingNullVars(Class parameter);

                                            QuadConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.quad;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;

                                        public interface QuadConstraintStream extends ConstraintStream {
                                            void ifExistsIncludingNullVars(Class parameter);
                                            void ifNotExistsIncludingNullVars(Class parameter);

                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;

                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

                                        public interface ConstraintFactory {
                                            UniConstraintStream forEach(Class parameter);
                                            void forEachIncludingNullVars(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.domain.variable;

                                        public @interface PlanningVariable {
                                            boolean nullable();
                                        }"""));
    }

    @Test
    void planningVariable() {
        rewriteRun(java(
                wrapVariable("nullable"),
                wrapVariable("allowsUnassigned")));
    }

    @Test
    void uniConstraintFactory() {
        rewriteRun(java(
                wrapStream("return f.forEachIncludingNullVars(String.class)"),
                wrapStream("return f.forEachIncludingUnassigned(String.class)")));
    }

    @Test
    void uniIfExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .ifExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .ifExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void uniIfExistsOther() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .ifExistsOtherIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .ifExistsOtherIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void uniIfNotExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .ifNotExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .ifNotExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void uniIfNotExistsOther() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .ifNotExistsOtherIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .ifNotExistsOtherIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void biIfExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .ifExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .ifExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void biIfNotExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void triIfExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void triIfNotExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void quadIfExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifExistsIncludingUnassigned(String.class);
                        """)));
    }

    @Test
    void quadIfNotExists() {
        rewriteRun(java(
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingNullVars(String.class);
                        """),
                wrapStream("""
                        return f.forEach(String.class)
                            .join(String.class)
                            .join(String.class)
                            .join(String.class)
                            .ifNotExistsIncludingUnassigned(String.class);
                        """)));
    }

    private static String wrapVariable(String content) {
        return """
                import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

                class Test {

                    @PlanningVariable(%s = true)
                    String variable = null;

                }""".formatted(content);
    }

    private static String wrapStream(String content) {
        return """
                import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
                import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
                import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
                import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
                import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;

                class Test {
                    ConstraintStream myConstraint(ConstraintFactory f) {
                        %s;
                    }
                }""".formatted(content);
    }

}
