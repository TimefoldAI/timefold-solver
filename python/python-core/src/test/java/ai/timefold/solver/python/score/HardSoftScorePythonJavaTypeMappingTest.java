package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HardSoftScorePythonJavaTypeMappingTest {
    HardSoftScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new HardSoftScorePythonJavaTypeMapping(PythonHardSoftScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonHardSoftScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(HardSoftLongScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = HardSoftLongScore.of(10, 2);

        var initializedPythonScore = (PythonHardSoftScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);
        assertThat(initializedPythonScore.hard_score).isEqualTo(PythonInteger.valueOf(10));
        assertThat(initializedPythonScore.soft_score).isEqualTo(PythonInteger.valueOf(2));

        var uninitializedScore = HardSoftLongScore.ofUninitialized(-300, 20, 1);
        var uninitializedPythonScore = (PythonHardSoftScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-300));
        assertThat(uninitializedPythonScore.hard_score).isEqualTo(PythonInteger.valueOf(20));
        assertThat(uninitializedPythonScore.soft_score).isEqualTo(PythonInteger.valueOf(1));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardSoftScore.of(10, 2);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.hardScore()).isEqualTo(10);
        assertThat(initializedJavaScore.softScore()).isEqualTo(2);

        var uninitializedScore = PythonHardSoftScore.ofUninitialized(-300, 20, 1);
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-300);
        assertThat(uninitializedJavaScore.hardScore()).isEqualTo(20);
        assertThat(uninitializedJavaScore.softScore()).isEqualTo(1);
    }
}