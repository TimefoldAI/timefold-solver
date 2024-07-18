package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class NullableRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NullableRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
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

    // ************************************************************************
    // Uni
    // ************************************************************************

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

    // ************************************************************************
    // Bi
    // ************************************************************************

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

    // ************************************************************************
    // Bi
    // ************************************************************************

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

    // ************************************************************************
    // Quad
    // ************************************************************************

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

    // ************************************************************************
    // Helper methods
    // ************************************************************************

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
