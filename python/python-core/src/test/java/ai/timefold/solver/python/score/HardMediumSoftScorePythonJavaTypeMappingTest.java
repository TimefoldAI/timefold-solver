package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HardMediumSoftScorePythonJavaTypeMappingTest {
    HardMediumSoftScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new HardMediumSoftScorePythonJavaTypeMapping(PythonHardMediumSoftScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonHardMediumSoftScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(HardMediumSoftLongScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = HardMediumSoftLongScore.of(300, 20, 1);

        var initializedPythonScore = (PythonHardMediumSoftScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);
        assertThat(initializedPythonScore.hard_score).isEqualTo(PythonInteger.valueOf(300));
        assertThat(initializedPythonScore.medium_score).isEqualTo(PythonInteger.valueOf(20));
        assertThat(initializedPythonScore.soft_score).isEqualTo(PythonInteger.valueOf(1));

        var uninitializedScore = HardMediumSoftLongScore.ofUninitialized(-4000, 300, 20, 1);
        var uninitializedPythonScore = (PythonHardMediumSoftScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-4000));
        assertThat(uninitializedPythonScore.hard_score).isEqualTo(PythonInteger.valueOf(300));
        assertThat(uninitializedPythonScore.medium_score).isEqualTo(PythonInteger.valueOf(20));
        assertThat(uninitializedPythonScore.soft_score).isEqualTo(PythonInteger.valueOf(1));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardMediumSoftScore.of(300, 20, 1);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.hardScore()).isEqualTo(300);
        assertThat(initializedJavaScore.mediumScore()).isEqualTo(20);
        assertThat(initializedJavaScore.softScore()).isEqualTo(1);

        var uninitializedScore = PythonHardMediumSoftScore.ofUninitialized(-4000, 300, 20, 1);
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-4000);
        assertThat(uninitializedJavaScore.hardScore()).isEqualTo(300);
        assertThat(uninitializedJavaScore.mediumScore()).isEqualTo(20);
        assertThat(uninitializedJavaScore.softScore()).isEqualTo(1);
    }
}