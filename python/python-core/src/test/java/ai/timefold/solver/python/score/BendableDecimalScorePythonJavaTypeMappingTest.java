package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BendableDecimalScorePythonJavaTypeMappingTest {
    BendableDecimalScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new BendableDecimalScorePythonJavaTypeMapping(PythonBendableDecimalScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonBendableDecimalScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(BendableBigDecimalScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = BendableBigDecimalScore.of(
                new BigDecimal[] { BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(30) },
                new BigDecimal[] { BigDecimal.valueOf(4), BigDecimal.valueOf(5) });

        var initializedPythonScore = (PythonBendableDecimalScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);

        assertThat(initializedPythonScore.hard_scores.size()).isEqualTo(3);
        assertThat(initializedPythonScore.hard_scores.get(0)).isEqualTo(PythonDecimal.valueOf("10"));
        assertThat(initializedPythonScore.hard_scores.get(1)).isEqualTo(PythonDecimal.valueOf("20"));
        assertThat(initializedPythonScore.hard_scores.get(2)).isEqualTo(PythonDecimal.valueOf("30"));

        assertThat(initializedPythonScore.soft_scores.size()).isEqualTo(2);
        assertThat(initializedPythonScore.soft_scores.get(0)).isEqualTo(PythonDecimal.valueOf("4"));
        assertThat(initializedPythonScore.soft_scores.get(1)).isEqualTo(PythonDecimal.valueOf("5"));

        var uninitializedScore = BendableBigDecimalScore.ofUninitialized(-300,
                new BigDecimal[] { BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(30) },
                new BigDecimal[] { BigDecimal.valueOf(4), BigDecimal.valueOf(5) });
        var uninitializedPythonScore = (PythonBendableDecimalScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-300));

        assertThat(uninitializedPythonScore.hard_scores.size()).isEqualTo(3);
        assertThat(uninitializedPythonScore.hard_scores.get(0)).isEqualTo(PythonDecimal.valueOf("10"));
        assertThat(uninitializedPythonScore.hard_scores.get(1)).isEqualTo(PythonDecimal.valueOf("20"));
        assertThat(uninitializedPythonScore.hard_scores.get(2)).isEqualTo(PythonDecimal.valueOf("30"));

        assertThat(uninitializedPythonScore.soft_scores.size()).isEqualTo(2);
        assertThat(uninitializedPythonScore.soft_scores.get(0)).isEqualTo(PythonDecimal.valueOf("4"));
        assertThat(uninitializedPythonScore.soft_scores.get(1)).isEqualTo(PythonDecimal.valueOf("5"));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonBendableDecimalScore.of(new int[] { 10, 20, 30 }, new int[] { 4, 5 });

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.hardScores()).containsExactly(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(30));
        assertThat(initializedJavaScore.softScores()).containsExactly(
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(5));

        var uninitializedScore = PythonBendableDecimalScore.ofUninitialized(-300, new int[] { 10, 20, 30 }, new int[] { 4, 5 });
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-300);
        assertThat(uninitializedJavaScore.hardScores()).containsExactly(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(30));
        assertThat(uninitializedJavaScore.softScores()).containsExactly(
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(5));
    }
}