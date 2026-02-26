package ai.timefold.solver.migration.one;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ConstraintStreamMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new FindMissingTypes(false), new ConstraintStreamMigrationRecipe())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
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
