package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HardMediumSoftDecimalScorePythonJavaTypeMappingTest {
    HardMediumSoftDecimalScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new HardMediumSoftDecimalScorePythonJavaTypeMapping(PythonHardMediumSoftDecimalScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonHardMediumSoftDecimalScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(HardMediumSoftBigDecimalScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = HardMediumSoftBigDecimalScore.of(BigDecimal.valueOf(300),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(1));

        var initializedPythonScore = (PythonHardMediumSoftDecimalScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.hard_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(300)));
        assertThat(initializedPythonScore.medium_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(20)));
        assertThat(initializedPythonScore.soft_score).isEqualTo(new PythonDecimal(BigDecimal.valueOf(1)));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonHardMediumSoftDecimalScore.of(300, 20, 1);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.hardScore()).isEqualTo(BigDecimal.valueOf(300));
        assertThat(initializedJavaScore.mediumScore()).isEqualTo(BigDecimal.valueOf(20));
        assertThat(initializedJavaScore.softScore()).isEqualTo(BigDecimal.valueOf(1));
    }

}