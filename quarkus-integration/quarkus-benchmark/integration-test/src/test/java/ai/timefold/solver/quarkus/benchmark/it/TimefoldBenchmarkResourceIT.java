package ai.timefold.solver.quarkus.benchmark.it;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Test various Timefold operations running in native mode
 */
@QuarkusIntegrationTest
@Disabled("timefold-solver-quarkus-benchmark cannot compile to native")
public class TimefoldBenchmarkResourceIT extends TimefoldBenchmarkResourceTest {

}
