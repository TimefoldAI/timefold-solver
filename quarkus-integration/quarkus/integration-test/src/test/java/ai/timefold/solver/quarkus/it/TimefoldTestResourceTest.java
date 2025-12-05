package ai.timefold.solver.quarkus.it;

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
                .post("/timefold/test/solver-factory")
                .then()
                .body(is(
                        "0hard/5soft"));
    }

    @Test
    @Timeout(600)
    void solveWithTimeOverride() {
        // Spent-time is 30s by default, but it is overridden with 10
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .get("/timefold/test/solver-factory/override?seconds=10")
                .then()
                .body(is(
                        "0hard/5soft,0.50"));
    }

}
