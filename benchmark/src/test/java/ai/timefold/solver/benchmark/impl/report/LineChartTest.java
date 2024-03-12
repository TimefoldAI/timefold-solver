package ai.timefold.solver.benchmark.impl.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.benchmark.impl.report.LineChart.Builder;
import ai.timefold.solver.core.impl.util.Pair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
            42179797364,    1000000000
            """)
    void stepSize(BigDecimal difference, BigDecimal expectedStepSize) {
        BigDecimal actualStepSize = LineChart.stepSize(BigDecimal.ZERO, difference).stripTrailingZeros();
        assertThat(actualStepSize)
                .isEqualTo(expectedStepSize.stripTrailingZeros());
    }

    @Test
    void testDownSamplingProcess() throws IOException {
        List<Double> xValues = new ArrayList<>(8392);
        try (BufferedReader in = new BufferedReader(new FileReader(
                LineChartTest.class.getResource("/ai/timefold/solver/benchmark/impl/result/xValues.csv").getFile()))) {
            String str;
            while ((str = in.readLine()) != null) {
                String[] values = str.split(",");
                for (String value : values) {
                    xValues.add(Double.parseDouble(value.trim()));
                }
            }
        }
        List<Double> yValues = new ArrayList<>(8392);
        try (BufferedReader in = new BufferedReader(new FileReader(
                LineChartTest.class.getResource("/ai/timefold/solver/benchmark/impl/result/yValues.csv").getFile()))) {
            String str;
            while ((str = in.readLine()) != null) {
                String[] values = str.split(",");
                for (String value : values) {
                    if (value.trim().equals("null")) {
                        yValues.add(null);
                    } else {
                        yValues.add(Double.parseDouble(value.trim()));
                    }
                }
            }
        }
        /**
         * The result was created by the implementation available at: https://github.com/drcrane/downsample
         */
        List<Pair<Double, Double>> expectedResult = new ArrayList<>(1282);
        try (BufferedReader in = new BufferedReader(new FileReader(
                LineChartTest.class.getResource("/ai/timefold/solver/benchmark/impl/result/resultValues.csv").getFile()))) {
            String str;
            while ((str = in.readLine()) != null) {
                String[] values = str.split(",");
                for (String value : values) {
                    String[] expected = value.split("/");
                    expectedResult.add(new Pair<>(Double.parseDouble(expected[0]), Double.parseDouble(expected[1])));
                }
            }
        }
        assertThat(xValues).isNotNull();
        assertThat(yValues).isNotNull();
        assertThat(expectedResult).isNotNull();

        Builder<Double, Double> builder = new Builder<>();
        for (int i = 0; i < xValues.size(); i++) {
            if (yValues.get(i) != null) {
                builder.add("test", xValues.get(i), yValues.get(i));
            }
        }
        LineChart<Double, Double> chart = builder
                .build("test.txt", "test", "x", "y", false, false, false);
        assertThat(chart.keys())
                .containsAll(expectedResult.stream().mapToDouble(Pair::key).boxed().toList());
        assertThat(chart.datasets().get(0).data().stream().filter(Objects::nonNull).toList())
                .containsAll(expectedResult.stream().mapToDouble(Pair::value).boxed().toList());
    }
}
