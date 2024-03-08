package ai.timefold.solver.quarkus.benchmark.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;

/**
 * Test various Timefold benchmarking operations running in Quarkus
 */

@QuarkusTest
class TimefoldBenchmarkTestResourceTest {

    @Test
    @Timeout(600)
    void benchmark() throws Exception {
        String benchmarkResultDirectory = RestAssured.given()
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
        assertThat(xmlPath.getBoolean(
                "plannerBenchmarkResult.solverBenchmarkResult.singleBenchmarkResult.subSingleBenchmarkResult.succeeded"))
                .isTrue();
    }

}
