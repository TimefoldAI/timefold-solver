package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BendableScorePythonJavaTypeMappingTest {
    BendableScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new BendableScorePythonJavaTypeMapping(PythonBendableScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonBendableScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(BendableLongScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = BendableLongScore.of(new long[] { 10, 20, 30 }, new long[] { 4, 5 });

        var initializedPythonScore = (PythonBendableScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);

        assertThat(initializedPythonScore.hard_scores.size()).isEqualTo(3);
        assertThat(initializedPythonScore.hard_scores.get(0)).isEqualTo(PythonInteger.valueOf(10));
        assertThat(initializedPythonScore.hard_scores.get(1)).isEqualTo(PythonInteger.valueOf(20));
        assertThat(initializedPythonScore.hard_scores.get(2)).isEqualTo(PythonInteger.valueOf(30));

        assertThat(initializedPythonScore.soft_scores.size()).isEqualTo(2);
        assertThat(initializedPythonScore.soft_scores.get(0)).isEqualTo(PythonInteger.valueOf(4));
        assertThat(initializedPythonScore.soft_scores.get(1)).isEqualTo(PythonInteger.valueOf(5));

        var uninitializedScore = BendableLongScore.ofUninitialized(-300, new long[] { 10, 20, 30 }, new long[] { 4, 5 });
        var uninitializedPythonScore = (PythonBendableScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-300));

        assertThat(uninitializedPythonScore.hard_scores.size()).isEqualTo(3);
        assertThat(uninitializedPythonScore.hard_scores.get(0)).isEqualTo(PythonInteger.valueOf(10));
        assertThat(uninitializedPythonScore.hard_scores.get(1)).isEqualTo(PythonInteger.valueOf(20));
        assertThat(uninitializedPythonScore.hard_scores.get(2)).isEqualTo(PythonInteger.valueOf(30));

        assertThat(uninitializedPythonScore.soft_scores.size()).isEqualTo(2);
        assertThat(uninitializedPythonScore.soft_scores.get(0)).isEqualTo(PythonInteger.valueOf(4));
        assertThat(uninitializedPythonScore.soft_scores.get(1)).isEqualTo(PythonInteger.valueOf(5));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonBendableScore.of(new int[] { 10, 20, 30 }, new int[] { 4, 5 });

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.hardScores()).containsExactly(10, 20, 30);
        assertThat(initializedJavaScore.softScores()).containsExactly(4, 5);

        var uninitializedScore = PythonBendableScore.ofUninitialized(-300, new int[] { 10, 20, 30 }, new int[] { 4, 5 });
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-300);
        assertThat(uninitializedJavaScore.hardScores()).containsExactly(10, 20, 30);
        assertThat(uninitializedJavaScore.softScores()).containsExactly(4, 5);
    }
}