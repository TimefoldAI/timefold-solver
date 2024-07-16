package ai.timefold.solver.python.score;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleDecimalScorePythonJavaTypeMappingTest {
    SimpleDecimalScorePythonJavaTypeMapping typeMapping;

    @BeforeEach
    void setUp() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException {
        this.typeMapping = new SimpleDecimalScorePythonJavaTypeMapping(PythonSimpleDecimalScore.TYPE);
    }

    @Test
    void getPythonType() {
        assertThat(typeMapping.getPythonType()).isEqualTo(PythonSimpleDecimalScore.TYPE);
    }

    @Test
    void getJavaType() {
        assertThat(typeMapping.getJavaType()).isEqualTo(SimpleBigDecimalScore.class);
    }

    @Test
    void toPythonObject() {
        var initializedScore = SimpleBigDecimalScore.of(BigDecimal.valueOf(10));

        var initializedPythonScore = (PythonSimpleDecimalScore) typeMapping.toPythonObject(initializedScore);

        assertThat(initializedPythonScore.init_score).isEqualTo(PythonInteger.ZERO);
        assertThat(initializedPythonScore.score).isEqualTo(PythonDecimal.valueOf("10"));

        var uninitializedScore = SimpleBigDecimalScore.ofUninitialized(-5, BigDecimal.valueOf(20));
        var uninitializedPythonScore = (PythonSimpleDecimalScore) typeMapping.toPythonObject(uninitializedScore);

        assertThat(uninitializedPythonScore.init_score).isEqualTo(PythonInteger.valueOf(-5));
        assertThat(uninitializedPythonScore.score).isEqualTo(PythonDecimal.valueOf("20"));
    }

    @Test
    void toJavaObject() {
        var initializedScore = PythonSimpleDecimalScore.of(10);

        var initializedJavaScore = typeMapping.toJavaObject(initializedScore);

        assertThat(initializedJavaScore.initScore()).isEqualTo(0);
        assertThat(initializedJavaScore.score()).isEqualTo(BigDecimal.valueOf(10));

        var uninitializedScore = PythonSimpleDecimalScore.ofUninitialized(-5, 20);
        var uninitializedJavaScore = typeMapping.toJavaObject(uninitializedScore);

        assertThat(uninitializedJavaScore.initScore()).isEqualTo(-5);
        assertThat(uninitializedJavaScore.score()).isEqualTo(BigDecimal.valueOf(20));
    }
}