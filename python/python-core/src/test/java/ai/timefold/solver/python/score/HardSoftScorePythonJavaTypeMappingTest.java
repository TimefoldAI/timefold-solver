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

        assertThat(initializedPythonScore.hard_score).isEqualTo(PythonInteger.valueOf(10));
        assertThat(initializedPythonScore.soft_score).isEqualTo(PythonInteger.valueOf(2));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardSoftScore.of(10, 2);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.hardScore()).isEqualTo(10);
        assertThat(initializedJavaScore.softScore()).isEqualTo(2);
    }
}