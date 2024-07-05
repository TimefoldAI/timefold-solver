package ai.timefold.solver.quarkus.benchmark.it;

import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.xml.XmlPath;

/**
 * Test the benchmark for typical solver configs (blueprint).
 */
@QuarkusTest
@TestProfile(TimefoldBenchmarkBlueprintTest.BlueprintTestProfile.class)
class TimefoldBenchmarkBlueprintTest {

    @Test
    void benchmark() throws Exception {
        RestAssuredConfig timeoutConfig = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(SO_TIMEOUT, 10000));
        String benchmarkResultDirectory = RestAssured
                .given()
                .config(timeoutConfig)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .when()
                .post("/timefold/test/benchmark")
                .body().asString();
        assertThat(benchmarkResultDirectory).isNotNull();
        Path benchmarkResultDirectoryPath = Path.of(benchmarkResultDirectory);
        assertThat(Files.isDirectory(benchmarkResultDirectoryPath)).isTrue();
        Path benchmarkResultPath = Files.walk(benchmarkResultDirectoryPath, 2)
                .filter(path -> path.endsWith("plannerBenchmarkResult.xml")).findFirst().orElseThrow();
        assertThat(Files.isRegularFile(benchmarkResultPath)).isTrue();
        XmlPath xmlPath = XmlPath.from(benchmarkResultPath.toFile());
        assertThat(xmlPath
                .getList(
                        "plannerBenchmarkResult.solverBenchmarkResult.singleBenchmarkResult.subSingleBenchmarkResult.succeeded")
                .stream().anyMatch(node -> node.equals("true")))
                .isTrue();
    }

    public static class BlueprintTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> overrides = new HashMap<>(QuarkusTestProfile.super.getConfigOverrides());
            overrides.put("quarkus.timefold.benchmark.solver-benchmark-config-xml", "blueprintSolverBenchmarkConfig.xml");
            return overrides;
        }
    }
}
