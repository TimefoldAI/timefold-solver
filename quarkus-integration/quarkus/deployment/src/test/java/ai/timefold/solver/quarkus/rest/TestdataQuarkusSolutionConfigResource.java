package ai.timefold.solver.quarkus.rest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

@Path("/solver-config")
@ApplicationScoped
public class TestdataQuarkusSolutionConfigResource {
    @Inject
    @Named("solver1")
    SolverManager<TestdataQuarkusSolution, Long> solver1;

    @Inject
    @Named("solver2")
    SolverManager<TestdataQuarkusSolution, Long> solver2;

    @GET
    @Path("/seconds-spent-limit")
    @Produces(MediaType.TEXT_PLAIN)
    public String secondsSpentLimit() {
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);

        SolverScope<TestdataQuarkusSolution> solverScope = mock(SolverScope.class);
        doReturn(500L).when(solverScope).calculateTimeMillisSpentUpToNow();

        // Solver 1
        DefaultSolverJob<TestdataQuarkusSolution, Long> jobSolver1 =
                (DefaultSolverJob<TestdataQuarkusSolution, Long>) solver1.solve(1L, new TestdataQuarkusSolution());
        double gradientTimeSolver1 = jobSolver1.getSolverTermination().calculateSolverTimeGradient(solverScope);

        // Solver 2
        DefaultSolverJob<TestdataQuarkusSolution, Long> jobSolver2 =
                (DefaultSolverJob<TestdataQuarkusSolution, Long>) solver2.solve(2L, new TestdataQuarkusSolution());
        double gradientTimeSolver2 = jobSolver2.getSolverTermination().calculateSolverTimeGradient(solverScope);

        return String.format("secondsSpentLimit=%s;secondsSpentLimit=%s", decimalFormat.format(gradientTimeSolver1),
                decimalFormat.format(gradientTimeSolver2));
    }
}
