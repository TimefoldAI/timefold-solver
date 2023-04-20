package ai.timefold.solver.quarkus.jackson.it;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.jackson.it.domain.ITestdataPlanningSolution;

@Path("/timefold/test")
public class TimefoldTestResource {

    @Inject
    SolverManager<ITestdataPlanningSolution, Long> solverManager;

    @POST
    @Path("/solver-factory")
    public ITestdataPlanningSolution solveWithSolverFactory(ITestdataPlanningSolution problem) {
        SolverJob<ITestdataPlanningSolution, Long> solverJob = solverManager.solve(1L, problem);
        try {
            return solverJob.getFinalBestSolution();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Solving failed.", e);
        }
    }

}
