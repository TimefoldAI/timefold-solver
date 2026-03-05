package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

@Execution(ExecutionMode.CONCURRENT)
class GeneralMethodChangeNameMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralMethodChangeNameMigrationRecipe())
                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.benchmark.api;
                                        public class PlannerBenchmark {
                                            void benchmarkAndShowReportInBrowser();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                         public class Constraint {
                                            void getConstraintName();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver;
                                         public class Solver {
                                            boolean isEveryProblemFactChangeProcessed();
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.solver.event;
                                         public class BestSolutionChangedEvent {
                                            boolean isEveryProblemFactChangeProcessed();
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.move.composite;
                                        import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
                                        import java.util.List;
                                         public class CartesianProductMoveSelectorConfig {
                                            List<MoveSelectorConfig> getMoveSelectorConfigList();
                                            void setMoveSelectorConfigList(List<MoveSelectorConfig> moveSelectorConfigList);
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.heuristic.selector.move.composite;
                                        import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
                                        import java.util.List;
                                         public class UnionMoveSelectorConfig {
                                            List<MoveSelectorConfig> getMoveSelectorConfigList();
                                            void setMoveSelectorConfigList(List<MoveSelectorConfig> moveSelectorConfigList);
                                        }"""));
    }

    @Test
    void migrate() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.benchmark.api.PlannerBenchmark;
                        import ai.timefold.solver.core.api.score.stream.Constraint;
                        import ai.timefold.solver.core.api.solver.Solver;
                        import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
                        import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
                        import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;

                        public class Test {
                                PlannerBenchmark benchmark;
                                Constraint constraint;
                                Solver solver;
                                BestSolutionChangedEvent bestSolutionChangedEvent;
                                CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig;
                                UnionMoveSelectorConfig unionMoveSelectorConfig;
                                public void test() {
                                    benchmark.benchmarkAndShowReportInBrowser();
                                    constraint.getConstraintName();
                                    solver.isEveryProblemFactChangeProcessed();
                                    bestSolutionChangedEvent.isEveryProblemFactChangeProcessed();
                                    cartesianProductMoveSelectorConfig.getMoveSelectorConfigList();
                                    cartesianProductMoveSelectorConfig.setMoveSelectorConfigList(null);
                                    unionMoveSelectorConfig.getMoveSelectorConfigList();
                                    unionMoveSelectorConfig.setMoveSelectorConfigList(null);
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.benchmark.api.PlannerBenchmark;
                        import ai.timefold.solver.core.api.score.stream.Constraint;
                        import ai.timefold.solver.core.api.solver.Solver;
                        import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
                        import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
                        import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;

                        public class Test {
                                PlannerBenchmark benchmark;
                                Constraint constraint;
                                Solver solver;
                                BestSolutionChangedEvent bestSolutionChangedEvent;
                                CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig;
                                UnionMoveSelectorConfig unionMoveSelectorConfig;
                                public void test() {
                                    benchmark.benchmark();
                                    constraint.getConstraintRef().constraintName();
                                    solver.isEveryProblemChangeProcessed();
                                    bestSolutionChangedEvent.isEveryProblemChangeProcessed();
                                    cartesianProductMoveSelectorConfig.getMoveSelectorList();
                                    cartesianProductMoveSelectorConfig.setMoveSelectorList(null);
                                    unionMoveSelectorConfig.getMoveSelectorList();
                                    unionMoveSelectorConfig.setMoveSelectorList(null);
                                }
                        }"""));
    }

}
