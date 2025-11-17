package ai.timefold.solver.migration.v8;

import static org.openrewrite.java.Assertions.java;

import ai.timefold.solver.migration.AbstractRecipe;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class ScoreGettersRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ScoreGettersRecipe())
                .parser(AbstractRecipe.JAVA_PARSER);
    }

    @Test
    void bendableScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.bendable.BendableScore",
                "BendableScore score = BendableScore.of(new int[] {1, 2}, new int[] {3, 4});",
                """
                        int scoreLevelsSize = score.getLevelsSize();
                        int hardScoreLevelsSize = score.getHardLevelsSize();
                        int[] hardScores = score.getHardScores();
                        int hardScore0 = score.getHardScore(0);
                        int softScoreLevelsSize = score.getSoftLevelsSize();
                        int[] softScores = score.getSoftScores();
                        int softScore1 = score.getSoftScore(1);
                        int initScore = score.getInitScore();
                        """,
                """
                        int scoreLevelsSize = score.levelsSize();
                        int hardScoreLevelsSize = score.hardLevelsSize();
                        int[] hardScores = score.hardScores();
                        int hardScore0 = score.hardScore(0);
                        int softScoreLevelsSize = score.softLevelsSize();
                        int[] softScores = score.softScores();
                        int softScore1 = score.softScore(1);
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void bendableBigDecimalScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore",
                """
                        BendableBigDecimalScore score = BendableBigDecimalScore.of(
                           new BigDecimal[] {BigDecimal.ONE},
                           new BigDecimal[] {BigDecimal.ONE, BigDecimal.TEN});\
                        """,
                """
                        int scoreLevelsSize = score.getLevelsSize();
                        int hardScoreLevelsSize = score.getHardLevelsSize();
                        BigDecimal[] hardScores = score.getHardScores();
                        BigDecimal hardScore0 = score.getHardScore(0);
                        int softScoreLevelsSize = score.getSoftLevelsSize();
                        BigDecimal[] softScores = score.getSoftScores();
                        BigDecimal softScore1 = score.getSoftScore(1);
                        int initScore = score.getInitScore();
                        """,
                """
                        int scoreLevelsSize = score.levelsSize();
                        int hardScoreLevelsSize = score.hardLevelsSize();
                        BigDecimal[] hardScores = score.hardScores();
                        BigDecimal hardScore0 = score.hardScore(0);
                        int softScoreLevelsSize = score.softLevelsSize();
                        BigDecimal[] softScores = score.softScores();
                        BigDecimal softScore1 = score.softScore(1);
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void bendableLongScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore",
                "BendableLongScore score = BendableLongScore.of(" +
                        "   new long[] {1L}, " +
                        "   new long[] {1L, 10L});",
                """
                        int scoreLevelsSize = score.getLevelsSize();
                        int hardScoreLevelsSize = score.getHardLevelsSize();
                        long[] hardScores = score.getHardScores();
                        long hardScore0 = score.getHardScore(0);
                        int softScoreLevelsSize = score.getSoftLevelsSize();
                        long[] softScores = score.getSoftScores();
                        long softScore1 = score.getSoftScore(1);
                        int initScore = score.getInitScore();
                        """,
                """
                        int scoreLevelsSize = score.levelsSize();
                        int hardScoreLevelsSize = score.hardLevelsSize();
                        long[] hardScores = score.hardScores();
                        long hardScore0 = score.hardScore(0);
                        int softScoreLevelsSize = score.softLevelsSize();
                        long[] softScores = score.softScores();
                        long softScore1 = score.softScore(1);
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardMediumSoftScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore",
                "HardMediumSoftScore score = HardMediumSoftScore.of(1, 2, 3);",
                """
                        int hardScore = score.getHardScore();
                        int mediumScore = score.getMediumScore();
                        int softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        int hardScore = score.hardScore();
                        int mediumScore = score.mediumScore();
                        int softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardMediumSoftBigDecimalScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore",
                "HardMediumSoftBigDecimalScore score = HardMediumSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);",
                """
                        BigDecimal hardScore = score.getHardScore();
                        BigDecimal mediumScore = score.getMediumScore();
                        BigDecimal softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        BigDecimal hardScore = score.hardScore();
                        BigDecimal mediumScore = score.mediumScore();
                        BigDecimal softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardMediumSoftLongScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore",
                "HardMediumSoftLongScore score = HardMediumSoftLongScore.of(1L, 2L, 3L);",
                """
                        long hardScore = score.getHardScore();
                        long mediumScore = score.getMediumScore();
                        long softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        long hardScore = score.hardScore();
                        long mediumScore = score.mediumScore();
                        long softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardSoftScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore",
                "HardSoftScore score = HardSoftScore.of(1, 2);",
                """
                        int hardScore = score.getHardScore();
                        int softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        int hardScore = score.hardScore();
                        int softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardSoftBigDecimalScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore",
                "HardSoftBigDecimalScore score = HardSoftBigDecimalScore.of(BigDecimal.ZERO, BigDecimal.ONE);",
                """
                        BigDecimal hardScore = score.getHardScore();
                        BigDecimal softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        BigDecimal hardScore = score.hardScore();
                        BigDecimal softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void hardSoftLongScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore",
                "HardSoftLongScore score = HardSoftLongScore.of(1L, 2L);",
                """
                        long hardScore = score.getHardScore();
                        long softScore = score.getSoftScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        long hardScore = score.hardScore();
                        long softScore = score.softScore();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void simpleScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.simple.SimpleScore",
                "SimpleScore score = SimpleScore.of(1);",
                """
                        int value = score.getScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        int value = score.score();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void simpleBigDecimalScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore",
                "SimpleBigDecimalScore score = SimpleBigDecimalScore.of(BigDecimal.ONE);",
                """
                        BigDecimal value = score.getScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        BigDecimal value = score.score();
                        int initScore = score.initScore();
                        """);
    }

    @Test
    void simpleLongScore() {
        runTest("ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore",
                "SimpleLongScore score = SimpleLongScore.of(1L);",
                """
                        long value = score.getScore();
                        int initScore = score.getInitScore();
                        """,
                """
                        long value = score.score();
                        int initScore = score.initScore();
                        """);
    }

    private void runTest(String scoreImplClassFqn, @Language("java") String scoreDeclaration, @Language("java") String before, @Language("java") String after) {
        rewriteRun(java(wrap(scoreImplClassFqn, scoreDeclaration, before), wrap(scoreImplClassFqn, scoreDeclaration, after)));
    }

    private static @Language("java") String wrap(String scoreImplClassFqn, @Language("java") String scoreDeclaration, @Language("java") String content) {
        return "import java.math.BigDecimal;\n" +
                "import " + scoreImplClassFqn + ";\n" +
                "\n" +
                "class Test {\n" +
                "    public static void main(String[] args) {\n" +
                scoreDeclaration.trim() + "\n" +
                content.trim() + "\n" +
                "    }" +
                "}\n";
    }

}
