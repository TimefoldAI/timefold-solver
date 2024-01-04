package ai.timefold.solver.quarkus.it.reflection;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.it.reflection.domain.TestdataReflectionEntity;
import ai.timefold.solver.quarkus.it.reflection.domain.TestdataReflectionSolution;

@Path("/timefold/test")
public class TimefoldTestResource {

    @Inject
    SolverManager<TestdataReflectionSolution, Long> solverManager;

    @POST
    @Path("/solver-factory")
    @Produces(MediaType.TEXT_PLAIN)
    public String solveWithSolverFactory() {
        TestdataReflectionSolution planningProblem = new TestdataReflectionSolution();
        planningProblem.setEntityList(Arrays.asList(
                new TestdataReflectionEntity(),
                new TestdataReflectionEntity()));
        planningProblem.setFieldValueList(Arrays.asList("a", "bb", "ccc"));
        planningProblem.setMethodValueList(Arrays.asList("a", "bb", "ccc", "ddd"));
        SolverJob<TestdataReflectionSolution, Long> solverJob = solverManager.solve(1L, planningProblem);
        try {
            return solverJob.getFinalBestSolution().getScore().toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solving was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Solving failed.", e);
        }
    }
}
