package ai.timefold.solver.benchmark.impl.report;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

class LineChartTest {

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', textBlock = """
            # Difference,   Expected step size
            0.000012,       0.0000001
            0.000089,       0.000001
            0.00012,        0.000001
            0.0012,         0.00001
            0.012,          0.0001
            0.12,           0.001
            1,              0.01
            12,             0.1
            123,            1
            1234,           10
            12345,          100
            98765,          1000
            """)
    void stepSize(double difference, double expectedStepSize) {
        BigDecimal actualStepSize = LineChart.stepSize(0, difference).stripTrailingZeros();
        Assertions.assertThat(actualStepSize)
                .isEqualTo(BigDecimal.valueOf(expectedStepSize).stripTrailingZeros());
    }

}
