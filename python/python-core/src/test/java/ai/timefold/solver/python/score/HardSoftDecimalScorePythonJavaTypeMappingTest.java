package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HardSoftDecimalScorePythonJavaTypeMappingTest {
    HardSoftDecimalScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new HardSoftDecimalScorePythonJavaTypeMapping(PythonHardSoftDecimalScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonHardSoftDecimalScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(HardSoftBigDecimalScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = HardSoftBigDecimalScore.of(BigDecimal.valueOf(10), BigDecimal.valueOf(2));

        var initializedPythonScore = (PythonHardSoftDecimalScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);
        assertThat(initializedPythonScore.hard_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(10)));
        assertThat(initializedPythonScore.soft_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(2)));

        var uninitializedScore = HardSoftBigDecimalScore.ofUninitialized(-300, BigDecimal.valueOf(20), BigDecimal.valueOf(1));
        var uninitializedPythonScore = (PythonHardSoftDecimalScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-300));
        assertThat(uninitializedPythonScore.hard_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(20)));
        assertThat(uninitializedPythonScore.soft_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(1)));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardSoftDecimalScore.of(10, 2);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.hardScore()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(initializedJavaScore.softScore()).isEqualTo(BigDecimal.valueOf(2));

        var uninitializedScore = PythonHardSoftDecimalScore.ofUninitialized(-300, 20, 1);
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-300);
        assertThat(uninitializedJavaScore.hardScore()).isEqualTo(BigDecimal.valueOf(20));
        assertThat(uninitializedJavaScore.softScore()).isEqualTo(BigDecimal.valueOf(1));
    }
}