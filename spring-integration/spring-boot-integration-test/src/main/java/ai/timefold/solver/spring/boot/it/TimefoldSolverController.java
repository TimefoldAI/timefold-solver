package ai.timefold.solver.spring.boot.it;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.spring.boot.it.domain.IntegrationTestSolution;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integration-test")
public class TimefoldSolverController {
    private final SolverFactory<IntegrationTestSolution> solverFactory;

    public TimefoldSolverController(SolverFactory<IntegrationTestSolution> solverFactory) {
        this.solverFactory = solverFactory;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public IntegrationTestSolution solve(@RequestBody IntegrationTestSolution problem) {
        return solverFactory.buildSolver().solve(problem);
    }
}
