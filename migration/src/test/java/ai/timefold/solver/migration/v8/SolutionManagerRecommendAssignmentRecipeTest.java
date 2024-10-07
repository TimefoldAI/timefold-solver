package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class SolutionManagerRecommendAssignmentRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SolutionManagerRecommendAssignmentRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void base() {
        runTest("List<RecommendedFit> recommendations = solutionManager.recommendFit(null, null, null, null);",
                "List<RecommendedAssignment> recommendations = solutionManager.recommendAssignment(null, null, null, null);");
    }

    @Test
    void overload() {
        runTest("List<RecommendedFit> recommendations = solutionManager.recommendFit(null, null, null);",
                "List<RecommendedAssignment> recommendations = solutionManager.recommendAssignment(null, null, null);");
    }

    private void runTest(String before, String after) {
        rewriteRun(java(
                wrap(before, true),
                wrap(after, false)));
    }

    private static String wrap(String content, boolean addImport) {
        return "import ai.timefold.solver.core.api.solver.SolutionManager;\n" +
                "import ai.timefold.solver.core.api.solver.SolverFactory;\n" +
                (addImport ? "import ai.timefold.solver.core.api.solver.RecommendedFit;\n" : "") +
                "import ai.timefold.solver.core.api.solver.RecommendedAssignment;\n" +
                "import java.util.List;\n" +
                "\n" +
                "class Test {\n" +
                "    public static void main(String[] args) {\n" +
                "       SolverFactory solverFactory = SolverFactory.create(null);\n" +
                "       SolutionManager solutionManager = SolutionManager.create(solverFactory);\n" +
                "       " + content.trim() + "\n" +
                "    }" +
                "}\n";
    }

}
