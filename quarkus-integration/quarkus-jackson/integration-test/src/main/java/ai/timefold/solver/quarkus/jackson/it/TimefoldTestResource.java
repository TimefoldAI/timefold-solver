package ai.timefold.solver.quarkus.jackson.it;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.jackson.it.domain.ITestdataPlanningSolution;

@Path("/timefold/test")
public class TimefoldTestResource {

    private final SolverManager<ITestdataPlanningSolution, Long> solverManager;

    @Inject
    public TimefoldTestResource(SolverManager<ITestdataPlanningSolution, Long> solverManager) {
        this.solverManager = solverManager;
    }

    @POST
    @Path("/solver-factory")
    public ITestdataPlanningSolution solveWithSolverFactory(ITestdataPlanningSolution problem) {
        SolverJob<ITestdataPlanningSolution, Long> solverJob = solverManager.solve(1L, problem);
        try {
            return solverJob.getFinalBestSolution();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solving was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Solving failed.", e);
        }
    }

}
