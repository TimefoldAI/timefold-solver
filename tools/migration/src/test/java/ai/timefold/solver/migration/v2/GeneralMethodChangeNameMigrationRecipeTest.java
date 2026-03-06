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
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.uni;

                                        import java.util.function.ToLongFunction;

                                        public class UniConstraintStream {
                                           public void penalizeLong(Object constraintWeight) {}
                                           public void penalizeLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void rewardLong(Object constraintWeight) {}
                                           public void rewardLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void impactLong(Object constraintWeight) {}
                                           public void impactLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.bi;

                                        import java.util.function.ToLongFunction;

                                        public class BiConstraintStream {
                                           public void penalizeLong(Object constraintWeight) {}
                                           public void penalizeLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void rewardLong(Object constraintWeight) {}
                                           public void rewardLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void impactLong(Object constraintWeight) {}
                                           public void impactLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.tri;

                                        import java.util.function.ToLongFunction;

                                        public class TriConstraintStream {
                                           public void penalizeLong(Object constraintWeight) {}
                                           public void penalizeLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void rewardLong(Object constraintWeight) {}
                                           public void rewardLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void impactLong(Object constraintWeight) {}
                                           public void impactLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.quad;

                                        import java.util.function.ToLongFunction;

                                        public class QuadConstraintStream {
                                           public void penalizeLong(Object constraintWeight) {}
                                           public void penalizeLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void rewardLong(Object constraintWeight) {}
                                           public void rewardLong(Object constraintWeight, ToLongFunction matchWeigher) {}
                                           public void impactLong(Object constraintWeight) {}
                                           public void impactLong(Object constraintWeight, ToLongFunction matchWeigher) {}
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

    @Test
    void migrateConstraintStream() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;

                        public class Test {
                                void validate(UniConstraintStream stream, BiConstraintStream stream2, TriConstraintStream stream3, QuadConstraintStream stream4) {
                                    stream.penalizeLong(null);
                                    stream.penalizeLong(null, null);
                                    stream.rewardLong(null);
                                    stream.rewardLong(null, null);
                                    stream.impactLong(null);
                                    stream.impactLong(null, null);
                                    stream2.penalizeLong(null);
                                    stream2.penalizeLong(null, null);
                                    stream2.rewardLong(null);
                                    stream2.rewardLong(null, null);
                                    stream2.impactLong(null);
                                    stream2.impactLong(null, null);
                                    stream3.penalizeLong(null);
                                    stream3.penalizeLong(null, null);
                                    stream3.rewardLong(null);
                                    stream3.rewardLong(null, null);
                                    stream3.impactLong(null);
                                    stream3.impactLong(null, null);
                                    stream4.penalizeLong(null);
                                    stream4.penalizeLong(null, null);
                                    stream4.rewardLong(null);
                                    stream4.rewardLong(null, null);
                                    stream4.impactLong(null);
                                    stream4.impactLong(null, null);
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
                        import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;

                        public class Test {
                                void validate(UniConstraintStream stream, BiConstraintStream stream2, TriConstraintStream stream3, QuadConstraintStream stream4) {
                                    stream.penalize(null);
                                    stream.penalize(null, null);
                                    stream.reward(null);
                                    stream.reward(null, null);
                                    stream.impact(null);
                                    stream.impact(null, null);
                                    stream2.penalize(null);
                                    stream2.penalize(null, null);
                                    stream2.reward(null);
                                    stream2.reward(null, null);
                                    stream2.impact(null);
                                    stream2.impact(null, null);
                                    stream3.penalize(null);
                                    stream3.penalize(null, null);
                                    stream3.reward(null);
                                    stream3.reward(null, null);
                                    stream3.impact(null);
                                    stream3.impact(null, null);
                                    stream4.penalize(null);
                                    stream4.penalize(null, null);
                                    stream4.reward(null);
                                    stream4.reward(null, null);
                                    stream4.impact(null);
                                    stream4.impact(null, null);
                                }
                        }"""));
    }

}
