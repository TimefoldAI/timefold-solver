package ai.timefold.solver.quarks.it;

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
    void solveWithSolver1() throws Exception {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .post("/timefold/test/solver-factory/1")
                .then()
                .body(is(
                        "0hard/5soft"));
    }

    @Test
    @Timeout(600)
    void solveWithSolver2() throws Exception {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .post("/timefold/test/solver-factory/2")
                .then()
                .body(is(
                        "0hard/5soft"));
    }
}
