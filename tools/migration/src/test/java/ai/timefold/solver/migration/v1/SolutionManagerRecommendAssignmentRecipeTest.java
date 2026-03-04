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
class SolutionManagerRecommendAssignmentRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SolutionManagerRecommendAssignmentRecipe())
                .afterTypeValidationOptions(TypeValidation.none())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.solver;
                                        import ai.timefold.solver.core.api.solver.RecommendedFit;
                                        import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
                                        import java.util.List;
                                        import java.util.function.Function;

                                        public interface SolutionManager {
                                            SolutionManager create(Object parameter);
                                            List<RecommendedFit> recommendFit(Object parameter, Object secondParameter, Function thirdParameter, ScoreAnalysisFetchPolicy fourthParameter);
                                            List<RecommendedFit> recommendFit(Object parameter, Object secondParameter, Function thirdParameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;

                                        public interface SolverFactory {
                                            SolverFactory create(Object parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;

                                        public interface RecommendedFit {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;

                                        public interface RecommendedAssignment {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;

                                        public interface ScoreAnalysisFetchPolicy {
                                        }"""));
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
