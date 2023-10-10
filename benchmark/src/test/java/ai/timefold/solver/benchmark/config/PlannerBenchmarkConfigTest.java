package ai.timefold.solver.benchmark.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import ai.timefold.solver.benchmark.impl.io.PlannerBenchmarkConfigIO;
import ai.timefold.solver.core.impl.io.jaxb.TimefoldXmlSerializationException;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.jackson.impl.domain.solution.JacksonSolutionFileIO;
import ai.timefold.solver.persistence.common.api.domain.solution.RigidTestdataSolutionFileIO;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

class PlannerBenchmarkConfigTest {

    private static final String TEST_PLANNER_BENCHMARK_CONFIG_WITH_NAMESPACE = "testBenchmarkConfigWithNamespace.xml";
    private static final String TEST_PLANNER_BENCHMARK_CONFIG_WITHOUT_NAMESPACE = "testBenchmarkConfigWithoutNamespace.xml";

    @ParameterizedTest
    @ValueSource(strings = { TEST_PLANNER_BENCHMARK_CONFIG_WITHOUT_NAMESPACE, TEST_PLANNER_BENCHMARK_CONFIG_WITH_NAMESPACE })
    void xmlConfigFileRemainsSameAfterReadWrite(String xmlBenchmarkConfigResource) throws IOException {
        PlannerBenchmarkConfigIO xmlIO = new PlannerBenchmarkConfigIO();
        PlannerBenchmarkConfig jaxbBenchmarkConfig;

        try (Reader reader = new InputStreamReader(
                PlannerBenchmarkConfigTest.class.getResourceAsStream(xmlBenchmarkConfigResource))) {
            jaxbBenchmarkConfig = xmlIO.read(reader);
        }

        assertThat(jaxbBenchmarkConfig).isNotNull();

        Writer stringWriter = new StringWriter();
        xmlIO.write(jaxbBenchmarkConfig, stringWriter);
        String jaxbString = stringWriter.toString();

        String originalXml = IOUtils.toString(PlannerBenchmarkConfigTest.class.getResourceAsStream(xmlBenchmarkConfigResource),
                StandardCharsets.UTF_8);

        // During writing the benchmark config, the benchmark element's namespace is removed.
        String benchmarkElementWithNamespace =
                PlannerBenchmarkConfig.XML_ELEMENT_NAME + " xmlns=\"" + PlannerBenchmarkConfig.XML_NAMESPACE + "\"";
        if (originalXml.contains(benchmarkElementWithNamespace)) {
            originalXml = originalXml.replace(benchmarkElementWithNamespace, PlannerBenchmarkConfig.XML_ELEMENT_NAME);
        }
        assertThat(jaxbString).isXmlEqualTo(originalXml);
    }

    @Test
    void readAndValidateInvalidBenchmarkConfig_failsIndicatingTheIssue() {
        PlannerBenchmarkConfigIO xmlIO = new PlannerBenchmarkConfigIO();
        String benchmarkConfigXml = "<plannerBenchmark xmlns=\"https://timefold.ai/xsd/benchmark\">\n"
                + "  <benchmarkDirectory>data</benchmarkDirectory>\n"
                + "  <parallelBenchmarkCount>AUTO</parallelBenchmarkCount>\n"
                + "  <solverBenchmark>\n"
                + "    <name>Entity Tabu Search</name>\n"
                + "    <solver>\n"
                // Intentionally wrong to simulate a typo.
                + "      <solutionKlazz>ai.timefold.solver.core.impl.testdata.domain.TestdataSolution</solutionKlazz>\n"
                + "      <entityClass>ai.timefold.solver.core.impl.testdata.domain.TestdataEntity</entityClass>\n"
                + "    </solver>\n"
                + "    <problemBenchmarks>\n"
                + "      <solutionFileIOClass>" + TestdataSolutionFileIO.class.getCanonicalName() + "</solutionFileIOClass>\n"
                + "      <inputSolutionFile>nonExistingDataset1.xml</inputSolutionFile>\n"
                + "    </problemBenchmarks>\n"
                + "  </solverBenchmark>\n"
                + "</plannerBenchmark>\n";

        StringReader stringReader = new StringReader(benchmarkConfigXml);
        assertThatExceptionOfType(TimefoldXmlSerializationException.class)
                .isThrownBy(() -> xmlIO.read(stringReader))
                .withRootCauseExactlyInstanceOf(SAXParseException.class)
                .withMessageContaining("solutionKlazz");
    }

    @Test
    public void assignCustomSolutionIO() {
        ProblemBenchmarksConfig pbc = new ProblemBenchmarksConfig();
        pbc.setSolutionFileIOClass(RigidTestdataSolutionFileIO.class);

        Class<? extends SolutionFileIO<?>> configured = pbc.getSolutionFileIOClass();
        assertThat(configured).isNotNull();
    }

    private static class TestdataSolutionFileIO extends JacksonSolutionFileIO<TestdataSolution> {
        private TestdataSolutionFileIO() {
            super(TestdataSolution.class);
        }
    }
}
