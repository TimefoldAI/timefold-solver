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
class ScoreManagerMethodsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ScoreManagerMethodsRecipe())
                .afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score;

                                        public interface ScoreManager {
                                            ScoreManager create(Object parameter);
                                            String getSummary(Object parameter);
                                            Object explainScore(Object parameter);
                                            Object updateScore(Object parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;

                                        public interface SolverFactory {
                                            SolverFactory create(Object parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score;

                                        public interface ScoreExplanation {
                                        }"""));
    }

    @Test
    void summary() {
        runTest("String summary = scoreManager.getSummary(solution);",
                "String summary = scoreManager.explain(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY).getSummary();");
    }

    @Test
    void explain() {
        runTest("Object explanation = scoreManager.explainScore(solution);",
                "Object explanation = scoreManager.explain(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);");
    }

    @Test
    void update() {
        runTest("Object score = scoreManager.updateScore(solution);",
                "Object score = scoreManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);");
    }

    private void runTest(String before, String after) {
        rewriteRun(java(
                wrap(before, false),
                wrap(after, true)));
    }

    private static String wrap(String content, boolean addImport) {
        return "import ai.timefold.solver.core.api.score.ScoreManager;\n" +
                (addImport ? "import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;\n" : "") +
                "import ai.timefold.solver.core.api.score.ScoreExplanation;\n" +
                "import ai.timefold.solver.core.api.solver.SolverFactory;\n" +
                "\n" +
                "class Test {\n" +
                "    public static void main(String[] args) {\n" +
                "       SolverFactory solverFactory = SolverFactory.create(null);\n" +
                "       ScoreManager scoreManager = ScoreManager.create(solverFactory);\n" +
                "       Object solution = null;\n" +
                "       " + content.trim() + "\n" +
                "    }" +
                "}\n";
    }

}
