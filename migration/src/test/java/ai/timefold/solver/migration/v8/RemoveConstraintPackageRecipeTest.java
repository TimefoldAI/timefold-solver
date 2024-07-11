package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class RemoveConstraintPackageRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveConstraintPackageRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void uni() {
        rewriteRun(
                java(
                        wrap("        return f.forEach(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package\", \"My constraint\");"),
                        wrap("        return f.forEach(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package.My constraint\");")));
    }

    @Test
    void bi() {
        rewriteRun(
                java(
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package\", \"My constraint\");"),
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package.My constraint\");")));
    }

    @Test
    void tri() {
        rewriteRun(
                java(
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package\", \"My constraint\");"),
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package.My constraint\");")));
    }

    @Test
    void quad() {
        rewriteRun(
                java(
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package\", \"My constraint\");"),
                        wrap("        return f.forEach(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .join(String.class)\n" +
                                "                .penalize(HardSoftScore.ONE_HARD)\n" +
                                "                .asConstraint(\"My package.My constraint\");")));
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
                "    Constraint myConstraint(ConstraintFactory f) {\n" +
                content + "\n" +
                "    }" +
                "}\n";
    }

}
