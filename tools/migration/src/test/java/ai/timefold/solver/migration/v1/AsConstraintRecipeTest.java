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
class AsConstraintRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new AsConstraintRecipe())
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
                                        import ai.timefold.solver.core.api.score.Score;
                                        import java.util.function.ToIntFunction;
                                        import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;

                                        public interface ConstraintStream {
                                            void penalize(String parameter, Score secondParameter);
                                            void penalize(String parameter, String secondParameter, Score thirdParameter);
                                            void penalizeConfigurable(String parameter);
                                            void penalizeConfigurable(String parameter, String secondParameter);
                                            void reward(String parameter, Score secondParameter);
                                            void reward(String parameter, String secondParameter, Score thirdParameter);
                                            void rewardConfigurable(String parameter);
                                            void rewardConfigurable(String parameter, String secondParameter);
                                            void impact(String parameter, Score secondParameter);
                                            void impact(String parameter, String secondParameter, Score thirdParameter);
                                            void impactConfigurable(String parameter);
                                            void impactConfigurable(String parameter, String secondParameter);

                                            BiConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.uni;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.Score;
                                        import java.util.function.ToIntFunction;
                                        import java.util.function.ToLongFunction;
                                        import java.util.function.Function;

                                        public interface UniConstraintStream extends ConstraintStream {
                                            void penalize(String parameter, Score secondParameter, ToIntFunction thirdParameter);
                                            void penalize(String parameter, String secondParameter, Score thirdParameter, ToIntFunction fourthParameter);
                                            void penalizeConfigurable(String parameter, ToIntFunction secondParameter);
                                            void penalizeConfigurable(String parameter, String secondParameter, ToIntFunction thirdParameter);
                                            void penalizeLong(String parameter, Score secondParameter, ToLongFunction thirdParameter);
                                            void penalizeLong(String parameter, String secondParameter, Score thirdParameter, ToLongFunction fourthParameter);
                                            void penalizeConfigurableLong(String parameter, ToLongFunction secondParameter);
                                            void penalizeConfigurableLong(String parameter, String secondParameter, ToLongFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, Score secondParameter, Function thirdParameter);
                                            void penalizeBigDecimal(String parameter, String secondParameter, Score thirdParameter, Function fourthParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, Function secondParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, String secondParameter, Function thirdParameter);

                                            void reward(String parameter, Score secondParameter, ToIntFunction thirdParameter);
                                            void reward(String parameter, String secondParameter, Score thirdParameter, ToIntFunction fourthParameter);
                                            void rewardConfigurable(String parameter, ToIntFunction secondParameter);
                                            void rewardConfigurable(String parameter, String secondParameter, ToIntFunction thirdParameter);
                                            void rewardLong(String parameter, Score secondParameter, ToLongFunction thirdParameter);
                                            void rewardLong(String parameter, String secondParameter, Score thirdParameter, ToLongFunction fourthParameter);
                                            void rewardConfigurableLong(String parameter, ToLongFunction secondParameter);
                                            void rewardConfigurableLong(String parameter, String secondParameter, ToLongFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, Score secondParameter, Function thirdParameter);
                                            void rewardBigDecimal(String parameter, String secondParameter, Score thirdParameter, Function fourthParameter);
                                            void rewardConfigurableBigDecimal(String parameter, Function secondParameter);
                                            void rewardConfigurableBigDecimal(String parameter, String secondParameter, Function thirdParameter);

                                            void impact(String parameter, Score secondParameter, ToIntFunction thirdParameter);
                                            void impact(String parameter, String secondParameter, Score thirdParameter, ToIntFunction fourthParameter);
                                            void impactConfigurable(String parameter, ToIntFunction secondParameter);
                                            void impactConfigurable(String parameter, String secondParameter, ToIntFunction thirdParameter);
                                            void impactLong(String parameter, Score secondParameter, ToLongFunction thirdParameter);
                                            void impactLong(String parameter, String secondParameter, Score thirdParameter, ToLongFunction fourthParameter);
                                            void impactConfigurableLong(String parameter, ToLongFunction secondParameter);
                                            void impactConfigurableLong(String parameter, String secondParameter, ToLongFunction thirdParameter);
                                            void impactBigDecimal(String parameter, Score secondParameter, Function thirdParameter);
                                            void impactBigDecimal(String parameter, String secondParameter, Score thirdParameter, Function fourthParameter);
                                            void impactConfigurableBigDecimal(String parameter, Function secondParameter);
                                            void impactConfigurableBigDecimal(String parameter, String secondParameter, Function thirdParameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.bi;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
                                        import java.util.function.ToIntBiFunction;
                                        import java.util.function.ToLongBiFunction;
                                        import java.util.function.BiFunction;

                                        public interface BiConstraintStream extends ConstraintStream {
                                            void penalize(String parameter, Score secondParameter, ToIntBiFunction thirdParameter);
                                            void penalize(String parameter, String secondParameter, Score thirdParameter, ToIntBiFunction fourthParameter);
                                            void penalizeConfigurable(String parameter, ToIntBiFunction secondParameter);
                                            void penalizeConfigurable(String parameter, String secondParameter, ToIntBiFunction thirdParameter);
                                            void penalizeLong(String parameter, Score secondParameter, ToLongBiFunction thirdParameter);
                                            void penalizeLong(String parameter, String secondParameter, Score thirdParameter, ToLongBiFunction fourthParameter);
                                            void penalizeConfigurableLong(String parameter, ToLongBiFunction secondParameter);
                                            void penalizeConfigurableLong(String parameter, String secondParameter, ToLongBiFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, Score secondParameter, BiFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, String secondParameter, Score thirdParameter, BiFunction fourthParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, BiFunction secondParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, String secondParameter, BiFunction thirdParameter);

                                            void reward(String parameter, Score secondParameter, ToIntBiFunction thirdParameter);
                                            void reward(String parameter, String secondParameter, Score thirdParameter, ToIntBiFunction fourthParameter);
                                            void rewardConfigurable(String parameter, ToIntBiFunction secondParameter);
                                            void rewardConfigurable(String parameter, String secondParameter, ToIntBiFunction thirdParameter);
                                            void rewardLong(String parameter, Score secondParameter, ToLongBiFunction thirdParameter);
                                            void rewardLong(String parameter, String secondParameter, Score thirdParameter, ToLongBiFunction fourthParameter);
                                            void rewardConfigurableLong(String parameter, ToLongBiFunction secondParameter);
                                            void rewardConfigurableLong(String parameter, String secondParameter, ToLongBiFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, Score secondParameter, BiFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, String secondParameter, Score thirdParameter, BiFunction fourthParameter);
                                            void rewardConfigurableBigDecimal(String parameter, BiFunction secondParameter);
                                            void rewardConfigurableBigDecimal(String parameter, String secondParameter, BiFunction thirdParameter);

                                            void impact(String parameter, Score secondParameter, ToIntBiFunction thirdParameter);
                                            void impact(String parameter, String secondParameter, Score thirdParameter, ToIntBiFunction fourthParameter);
                                            void impactConfigurable(String parameter, ToIntBiFunction secondParameter);
                                            void impactConfigurable(String parameter, String secondParameter, ToIntBiFunction thirdParameter);
                                            void impactLong(String parameter, Score secondParameter, ToLongBiFunction thirdParameter);
                                            void impactLong(String parameter, String secondParameter, Score thirdParameter, ToLongBiFunction fourthParameter);
                                            void impactConfigurableLong(String parameter, ToLongBiFunction secondParameter);
                                            void impactConfigurableLong(String parameter, String secondParameter, ToLongBiFunction thirdParameter);
                                            void impactBigDecimal(String parameter, Score secondParameter, BiFunction thirdParameter);
                                            void impactBigDecimal(String parameter, String secondParameter, Score thirdParameter, BiFunction fourthParameter);
                                            void impactConfigurableBigDecimal(String parameter, BiFunction secondParameter);
                                            void impactConfigurableBigDecimal(String parameter, String secondParameter, BiFunction thirdParameter);

                                            TriConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.tri;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.function.ToIntTriFunction;
                                        import ai.timefold.solver.core.api.function.ToLongTriFunction;
                                        import ai.timefold.solver.core.api.function.TriFunction;

                                        public interface TriConstraintStream extends ConstraintStream {
                                            void penalize(String parameter, Score secondParameter, ToIntTriFunction thirdParameter);
                                            void penalize(String parameter, String secondParameter, Score thirdParameter, ToIntTriFunction fourthParameter);
                                            void penalizeConfigurable(String parameter, ToIntTriFunction secondParameter);
                                            void penalizeConfigurable(String parameter, String secondParameter, ToIntTriFunction thirdParameter);
                                            void penalizeLong(String parameter, Score secondParameter, ToLongTriFunction thirdParameter);
                                            void penalizeLong(String parameter, String secondParameter, Score thirdParameter, ToLongTriFunction fourthParameter);
                                            void penalizeConfigurableLong(String parameter, ToLongTriFunction secondParameter);
                                            void penalizeConfigurableLong(String parameter, String secondParameter, ToLongTriFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, Score secondParameter, TriFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, String secondParameter, Score thirdParameter, TriFunction fourthParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, TriFunction secondParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, String secondParameter, TriFunction thirdParameter);

                                            void reward(String parameter, Score secondParameter, ToIntTriFunction thirdParameter);
                                            void reward(String parameter, String secondParameter, Score thirdParameter, ToIntTriFunction fourthParameter);
                                            void rewardConfigurable(String parameter, ToIntTriFunction secondParameter);
                                            void rewardConfigurable(String parameter, String secondParameter, ToIntTriFunction thirdParameter);
                                            void rewardLong(String parameter, Score secondParameter, ToLongTriFunction thirdParameter);
                                            void rewardLong(String parameter, String secondParameter, Score thirdParameter, ToLongTriFunction fourthParameter);
                                            void rewardConfigurableLong(String parameter, ToLongTriFunction secondParameter);
                                            void rewardConfigurableLong(String parameter, String secondParameter, ToLongTriFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, Score secondParameter, TriFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, String secondParameter, Score thirdParameter, TriFunction fourthParameter);
                                            void rewardConfigurableBigDecimal(String parameter, TriFunction secondParameter);
                                            void rewardConfigurableBigDecimal(String parameter, String secondParameter, TriFunction thirdParameter);

                                            void impact(String parameter, Score secondParameter, ToIntTriFunction thirdParameter);
                                            void impact(String parameter, String secondParameter, Score thirdParameter, ToIntTriFunction fourthParameter);
                                            void impactConfigurable(String parameter, ToIntTriFunction secondParameter);
                                            void impactConfigurable(String parameter, String secondParameter, ToIntTriFunction thirdParameter);
                                            void impactLong(String parameter, Score secondParameter, ToLongTriFunction thirdParameter);
                                            void impactLong(String parameter, String secondParameter, Score thirdParameter, ToLongTriFunction fourthParameter);
                                            void impactConfigurableLong(String parameter, ToLongTriFunction secondParameter);
                                            void impactConfigurableLong(String parameter, String secondParameter, ToLongTriFunction thirdParameter);
                                            void impactBigDecimal(String parameter, Score secondParameter, TriFunction thirdParameter);
                                            void impactBigDecimal(String parameter, String secondParameter, Score thirdParameter, TriFunction fourthParameter);
                                            void impactConfigurableBigDecimal(String parameter, TriFunction secondParameter);
                                            void impactConfigurableBigDecimal(String parameter, String secondParameter, TriFunction thirdParameter);

                                            QuadConstraintStream join(Class parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream.quad;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStream;
                                        import ai.timefold.solver.core.api.score.Score;
                                        import ai.timefold.solver.core.api.function.ToIntQuadFunction;
                                        import ai.timefold.solver.core.api.function.ToLongQuadFunction;
                                        import ai.timefold.solver.core.api.function.QuadFunction;

                                        public interface QuadConstraintStream extends ConstraintStream {
                                            void penalize(String parameter, Score secondParameter, ToIntQuadFunction thirdParameter);
                                            void penalize(String parameter, String secondParameter, Score thirdParameter, ToIntQuadFunction fourthParameter);
                                            void penalizeConfigurable(String parameter, ToIntQuadFunction secondParameter);
                                            void penalizeConfigurable(String parameter, String secondParameter, ToIntQuadFunction thirdParameter);
                                            void penalizeLong(String parameter, Score secondParameter, ToLongQuadFunction thirdParameter);
                                            void penalizeLong(String parameter, String secondParameter, Score thirdParameter, ToLongQuadFunction fourthParameter);
                                            void penalizeConfigurableLong(String parameter, ToLongQuadFunction secondParameter);
                                            void penalizeConfigurableLong(String parameter, String secondParameter, ToLongQuadFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, Score secondParameter, QuadFunction thirdParameter);
                                            void penalizeBigDecimal(String parameter, String secondParameter, Score thirdParameter, QuadFunction fourthParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, QuadFunction secondParameter);
                                            void penalizeConfigurableBigDecimal(String parameter, String secondParameter, QuadFunction thirdParameter);

                                            void reward(String parameter, Score secondParameter, ToIntQuadFunction thirdParameter);
                                            void reward(String parameter, String secondParameter, Score thirdParameter, ToIntQuadFunction fourthParameter);
                                            void rewardConfigurable(String parameter, ToIntQuadFunction secondParameter);
                                            void rewardConfigurable(String parameter, String secondParameter, ToIntQuadFunction thirdParameter);
                                            void rewardLong(String parameter, Score secondParameter, ToLongQuadFunction thirdParameter);
                                            void rewardLong(String parameter, String secondParameter, Score thirdParameter, ToLongQuadFunction fourthParameter);
                                            void rewardConfigurableLong(String parameter, ToLongQuadFunction secondParameter);
                                            void rewardConfigurableLong(String parameter, String secondParameter, ToLongQuadFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, Score secondParameter, QuadFunction thirdParameter);
                                            void rewardBigDecimal(String parameter, String secondParameter, Score thirdParameter, QuadFunction fourthParameter);
                                            void rewardConfigurableBigDecimal(String parameter, QuadFunction secondParameter);
                                            void rewardConfigurableBigDecimal(String parameter, String secondParameter, QuadFunction thirdParameter);

                                            void impact(String parameter, Score secondParameter, ToIntQuadFunction thirdParameter);
                                            void impact(String parameter, String secondParameter, Score thirdParameter, ToIntQuadFunction fourthParameter);
                                            void impactConfigurable(String parameter, ToIntQuadFunction secondParameter);
                                            void impactConfigurable(String parameter, String secondParameter, ToIntQuadFunction thirdParameter);
                                            void impactLong(String parameter, Score secondParameter, ToLongQuadFunction thirdParameter);
                                            void impactLong(String parameter, String secondParameter, Score thirdParameter, ToLongQuadFunction fourthParameter);
                                            void impactConfigurableLong(String parameter, ToLongQuadFunction secondParameter);
                                            void impactConfigurableLong(String parameter, String secondParameter, ToLongQuadFunction thirdParameter);
                                            void impactBigDecimal(String parameter, Score secondParameter, QuadFunction thirdParameter);
                                            void impactBigDecimal(String parameter, String secondParameter, Score thirdParameter, QuadFunction fourthParameter);
                                            void impactConfigurableBigDecimal(String parameter, QuadFunction secondParameter);
                                            void impactConfigurableBigDecimal(String parameter, String secondParameter, QuadFunction thirdParameter);

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
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.buildin.hardsoftlong;
                                        import ai.timefold.solver.core.api.score.Score;

                                        public interface HardSoftLongScore {
                                            public Score ONE_HARD;
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal;
                                        import ai.timefold.solver.core.api.score.Score;

                                        public interface HardSoftBigDecimalScore {
                                            public Score ONE_HARD;
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        @FunctionalInterface
                                        public interface ToIntTriFunction<A, B, C> {
                                            int applyAsInt(A a, B b, C c);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        @FunctionalInterface
                                        public interface ToLongTriFunction<A, B, C> {
                                            long applyAsLong(A a, B b, C c);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        @FunctionalInterface
                                        public interface TriFunction<A, B, C, R> {
                                            R apply(A a, B b, C c);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        @FunctionalInterface
                                        public interface ToIntQuadFunction<A, B, C, D> {
                                            int applyAsInt(A a, B b, C c, D d);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        @FunctionalInterface
                                        public interface ToLongQuadFunction<A, B, C, D> {
                                            long applyAsLong(A a, B b, C c, D d);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.function;

                                        public interface QuadFunction<A, B, C, D, R> {
                                            R apply(A a, B b, C c, D d);
                                        }"""));
    }

    // ************************************************************************
    // Uni
    // ************************************************************************

    @Test
    void uniPenalizeName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable("My constraint", (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable((a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurable((a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableLong("My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableLong((a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableLong((a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .penalizeConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable("My constraint", (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable((a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a) -> 7);
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurable((a) -> 7)
                                        .asConstraint("My package.My constraint");
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableLong("My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableLong((a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableLong((a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .rewardConfigurableBigDecimal((a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void uniImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void uniImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Bi
    // ************************************************************************

    @Test
    void biPenalizeName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void biImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void biImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Tri
    // ************************************************************************

    @Test
    void triPenalizeName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void triImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void triImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Quad
    // ************************************************************************

    @Test
    void quadPenalizeName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalize(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable("My package", "My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong("My package", "My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadPenalizeConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal("My package", "My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .penalizeConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint");\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable()
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .reward(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable("My package", "My constraint", (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurable((a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong("My package", "My constraint", (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableLong((a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadRewardConfigurableIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal("My package", "My constraint", (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .rewardConfigurableBigDecimal((a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactName() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactId() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherInt() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact("My package", "My constraint", HardSoftScore.ONE_HARD, (a, b, c, d) -> 7);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impact(HardSoftScore.ONE_HARD, (a, b, c, d) -> 7)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherLong() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong("My package", "My constraint", HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactLong(HardSoftLongScore.ONE_HARD, (a, b, c, d) -> 7L)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    @Test
    void quadImpactNameMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My constraint");\
                        """)));
    }

    @Test
    void quadImpactIdMatchWeigherBigDecimal() {
        rewriteRun(java(
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal("My package", "My constraint", HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN);\
                        """),
                wrap("""
                                f.forEach(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .join(String.class)
                                        .impactBigDecimal(HardSoftBigDecimalScore.ONE_HARD, (a, b, c, d) -> BigDecimal.TEN)
                                        .asConstraint("My package.My constraint");\
                        """)));
    }

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static String wrap(String content) {
        return "import java.math.BigDecimal;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;\n" +
                "import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;\n" +
                "import ai.timefold.solver.core.api.score.stream.ConstraintFactory;\n" +
                "import ai.timefold.solver.core.api.score.stream.Constraint;\n" +
                "\n" +
                "class Test {\n" +
                "    void myConstraint(ConstraintFactory f) {\n" +
                content + "\n" +
                "    }" +
                "}\n";
    }

}
