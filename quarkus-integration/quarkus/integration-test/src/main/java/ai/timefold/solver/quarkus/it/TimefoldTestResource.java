package ai.timefold.solver.quarkus.it;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

    private final SolverManager<TestdataStringLengthShadowSolution, Long> solverManager;

    @Inject
    public TimefoldTestResource(SolverManager<TestdataStringLengthShadowSolution, Long> solverManager) {
        this.solverManager = solverManager;
    }

    private static TestdataStringLengthShadowSolution generateProblem() {
        TestdataStringLengthShadowSolution planningProblem = new TestdataStringLengthShadowSolution();
        planningProblem.setEntityList(Arrays.asList(
                new TestdataStringLengthShadowEntity(),
                new TestdataStringLengthShadowEntity()));
        planningProblem.setValueList(Arrays.asList("a", "bb", "ccc"));
        return planningProblem;
    }

    @POST
    @Path("/solver-factory")
    @Produces(MediaType.TEXT_PLAIN)
    public String solveWithSolverFactory() {
        SolverJob<TestdataStringLengthShadowSolution, Long> solverJob = solverManager.solve(1L, generateProblem());
        try {
            return solverJob.getFinalBestSolution().getScore().toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solving was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Solving failed.", e);
        }
    }

    @GET
    @Path("/solver-factory/override")
    @Produces(MediaType.TEXT_PLAIN)
    public String solveWithOverriddenTime(@QueryParam("seconds") Integer seconds) {
        SolverJobBuilder<TestdataStringLengthShadowSolution, Long> solverJobBuilder = solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblem(generateProblem())
                .withConfigOverride(
                        new SolverConfigOverride<TestdataStringLengthShadowSolution>()
                                .withTerminationConfig(new TerminationConfig()
                                        .withSpentLimit(Duration.ofSeconds(seconds))));
        DefaultSolverJob<TestdataStringLengthShadowSolution, Long> solverJob =
                (DefaultSolverJob<TestdataStringLengthShadowSolution, Long>) solverJobBuilder.run();
        SolverScope<TestdataStringLengthShadowSolution> customScope = new SolverScope<>() {
            @Override
            public long calculateTimeMillisSpentUpToNow() {
                // Return five seconds to make the time gradient predictable
                return 5000L;
            }
        };
        // We ensure the best-score limit won't take priority
        customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
        customScope.setBestScore(HardSoftScore.of(-1, -1));
        try {
            String score = solverJob.getFinalBestSolution().getScore().toString();
            DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
            decimalFormatSymbols.setDecimalSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);
            double gradientTime = solverJob.getSolverTermination().calculateSolverTimeGradient(customScope);
            solverManager.terminateEarly(1L);
            return String.format("%s,%s", score, decimalFormat.format(gradientTime));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solving was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Solving failed.", e);
        }
    }
}
