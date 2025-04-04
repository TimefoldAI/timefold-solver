package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
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

        assertThat(initializedPythonScore.hard_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(10)));
        assertThat(initializedPythonScore.soft_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(2)));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardSoftDecimalScore.of(10, 2);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.hardScore()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(initializedJavaScore.softScore()).isEqualTo(BigDecimal.valueOf(2));
    }
}