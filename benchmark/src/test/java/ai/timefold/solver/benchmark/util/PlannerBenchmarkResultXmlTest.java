package ai.timefold.solver.benchmark.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlannerBenchmarkResultXmlTest {

    private static final String RESOURCE = "/dummyPlanner.xml";
    private final TestableBenchmarkResultIO io = new TestableBenchmarkResultIO();
    private static URL url;

    @BeforeAll
    static void setUp() {
        url = PlannerBenchmarkResultXmlTest.class.getResource(RESOURCE);
        assertThat(url).withFailMessage("Resource not found").isNotNull();
    }

    @Test
    void shouldDeserializeXml() throws Exception {
        File xmlFile = Paths.get(url.toURI()).toFile();
        PlannerBenchmarkResult result = io.read(xmlFile);

        assertThat(result.getUnifiedProblemBenchmarkResultList()).hasSize(1);

        @SuppressWarnings("unchecked")
        List<ProblemStatistic<?>> stats =
                (List<ProblemStatistic<?>>) result.getUnifiedProblemBenchmarkResultList()
                        .get(0)
                        .getProblemStatisticList();

        assertThat(stats)
                .extracting(ProblemStatistic::getProblemStatisticType)
                .containsExactlyInAnyOrder(
                        ProblemStatisticType.BEST_SCORE,
                        ProblemStatisticType.STEP_SCORE,
                        ProblemStatisticType.MEMORY_USE,
                        ProblemStatisticType.BEST_SOLUTION_MUTATION,
                        ProblemStatisticType.MOVE_COUNT_PER_STEP,
                        ProblemStatisticType.MOVE_COUNT_PER_TYPE,
                        ProblemStatisticType.SCORE_CALCULATION_SPEED,
                        ProblemStatisticType.MOVE_EVALUATION_SPEED);
    }

    @Test
    void shouldSerializeBackToIdenticalXml() throws Exception {
        // 1. load original XML text from resource
        String originalXml = Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8);

        // 2. deserialize + serialize
        PlannerBenchmarkResult bench = io.read(Paths.get(url.toURI()).toFile());
        Path tmp = Files.createTempFile("roundtrip", ".xml");
        io.write(tmp.toFile(), bench);

        String roundTripXml = Files.readString(tmp).trim();

        // 3. compare, ignoring insignificant whitespace
        assertThat(roundTripXml).isEqualToIgnoringWhitespace(originalXml.trim());
    }
}
