package ai.timefold.solver.quarkus.it;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.quarkus.it.domain.TestdataStringLengthShadowEntity;
import ai.timefold.solver.quarkus.it.domain.TestdataStringLengthShadowSolution;

@Path("/timefold/test")
public class TimefoldTestResource {

    @Named("solver1")
    SolverManager<TestdataStringLengthShadowSolution, Long> solverManager;

    @Named("solver2")
    SolverManager<TestdataStringLengthShadowSolution, Long> solverManager2;

    private TestdataStringLengthShadowSolution generateProblem() {
        TestdataStringLengthShadowSolution planningProblem = new TestdataStringLengthShadowSolution();
        planningProblem.setEntityList(Arrays.asList(
                new TestdataStringLengthShadowEntity(),
                new TestdataStringLengthShadowEntity()));
        planningProblem.setValueList(Arrays.asList("a", "bb", "ccc"));
        return planningProblem;
    }

    @POST
    @Path("/solver-factory/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String solveWithSolverFactory(@PathParam("id") String id) {
        SolverManager<TestdataStringLengthShadowSolution, Long> selectedSolverManager = solverManager;
        if (id.equals("2")) {
            selectedSolverManager = solverManager2;
        }
        SolverJob<TestdataStringLengthShadowSolution, Long> solverJob = selectedSolverManager.solve(1L, generateProblem());

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
