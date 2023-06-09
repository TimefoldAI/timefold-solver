package ai.timefold.solver.quarkus.jackson.it;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various Timefold operations running in Quarkus
 */

@QuarkusTest
class TimefoldTestResourceTest {

    @Test
    @Timeout(600)
    void solveWithSolverFactory() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{\"valueList\":[\"v1\",\"v2\"],\"entityList\":[{},{}]}")
                .post("/timefold/test/solver-factory")
                .then()
                .body(is(
                        "{\"valueList\":[\"v1\",\"v2\"],\"entityList\":[{\"value\":\"v1\"},{\"value\":\"v2\"}],\"score\":\"0\"}"));
    }

}
