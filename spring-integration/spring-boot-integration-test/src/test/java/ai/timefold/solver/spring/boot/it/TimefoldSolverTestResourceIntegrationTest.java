package ai.timefold.solver.spring.boot.it;

import java.util.List;

import ai.timefold.solver.spring.boot.it.domain.IntegrationTestEntity;
import ai.timefold.solver.spring.boot.it.domain.IntegrationTestSolution;
import ai.timefold.solver.spring.boot.it.domain.IntegrationTestValue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TimefoldSolverTestResourceIntegrationTest {

    @LocalServerPort
    String port;

    @Test
    void testSolve() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/integration-test")
                .build();

        IntegrationTestSolution problem = new IntegrationTestSolution(
                List.of(new IntegrationTestEntity("0"),
                        new IntegrationTestEntity("1"),
                        new IntegrationTestEntity("2")),
                List.of(new IntegrationTestValue("0"),
                        new IntegrationTestValue("1"),
                        new IntegrationTestValue("2")));
        client.post()
                .bodyValue(problem)
                .exchange()
                .expectBody()
                .jsonPath("score").isEqualTo("0")
                .jsonPath("entityList").isArray()
                .jsonPath("valueList").isArray()
                .jsonPath("entityList[0].id").isEqualTo("0")
                .jsonPath("entityList[0].value.id").isEqualTo("0")
                .jsonPath("entityList[1].id").isEqualTo("1")
                .jsonPath("entityList[1].value.id").isEqualTo("1")
                .jsonPath("entityList[2].id").isEqualTo("2")
                .jsonPath("entityList[2].value.id").isEqualTo("2");

    }
}
