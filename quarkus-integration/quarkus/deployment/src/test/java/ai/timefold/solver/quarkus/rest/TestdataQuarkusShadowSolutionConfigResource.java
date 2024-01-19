package ai.timefold.solver.quarkus.rest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

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
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableEntity;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;

@Path("/solver-config")
@ApplicationScoped
public class TestdataQuarkusShadowSolutionConfigResource {
    @Inject
    @Named("solver1")
    SolverManager<TestdataQuarkusSolution, Long> solver1;

    @Inject
    @Named("solver2")
    SolverManager<TestdataQuarkusShadowVariableSolution, Long> solver2;

    private static long count = 0;

    @GET
    @Path("/seconds-spent-limit")
    @Produces(MediaType.TEXT_PLAIN)
    public String secondsSpentLimit() {
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);

        // Solver 1
        TestdataQuarkusSolution problem = new TestdataQuarkusSolution();
        problem.setValueList(List.of("v1"));
        problem.setEntityList(List.of(new TestdataQuarkusEntity()));
        SolverScope<TestdataQuarkusSolution> solverScopeSolver1 = mock(SolverScope.class);
        doReturn(500L).when(solverScopeSolver1).calculateTimeMillisSpentUpToNow();
        DefaultSolverJob<TestdataQuarkusSolution, Long> jobSolver1 =
                (DefaultSolverJob<TestdataQuarkusSolution, Long>) solver1.solve(++count, problem);
        double gradientTimeSolver1 = jobSolver1.getSolverTermination().calculateSolverTimeGradient(solverScopeSolver1);

        // Solver 2
        TestdataQuarkusShadowVariableSolution problemShadowVariable = new TestdataQuarkusShadowVariableSolution();
        problemShadowVariable.setValueList(List.of("v1"));
        problemShadowVariable.setEntityList(List.of(new TestdataQuarkusShadowVariableEntity()));
        SolverScope<TestdataQuarkusShadowVariableSolution> solverScopeSolver2 = mock(SolverScope.class);
        doReturn(500L).when(solverScopeSolver2).calculateTimeMillisSpentUpToNow();
        DefaultSolverJob<TestdataQuarkusShadowVariableSolution, Long> jobSolver2 =
                (DefaultSolverJob<TestdataQuarkusShadowVariableSolution, Long>) solver2.solve(++count, problemShadowVariable);
        double gradientTimeSolver2 = jobSolver2.getSolverTermination().calculateSolverTimeGradient(solverScopeSolver2);

        return String.format("secondsSpentLimit=%s;secondsSpentLimit=%s", decimalFormat.format(gradientTimeSolver1),
                decimalFormat.format(gradientTimeSolver2));
    }
}
