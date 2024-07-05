package ai.timefold.solver.quarkus.benchmark.it;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkException;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.quarkus.benchmark.it.domain.TestdataListValueShadowEntity;
import ai.timefold.solver.quarkus.benchmark.it.domain.TestdataStringLengthShadowEntity;
import ai.timefold.solver.quarkus.benchmark.it.domain.TestdataStringLengthShadowSolution;

@Path("/timefold/test")
public class TimefoldBenchmarkTestResource {

    private final PlannerBenchmarkFactory benchmarkFactory;

    @Inject
    public TimefoldBenchmarkTestResource(PlannerBenchmarkFactory benchmarkFactory) {
        this.benchmarkFactory = benchmarkFactory;
    }

    @POST
    @Path("/benchmark")
    @Produces(MediaType.TEXT_PLAIN)
    public String benchmark() {
        TestdataStringLengthShadowSolution planningProblem = new TestdataStringLengthShadowSolution();
        planningProblem.setEntityList(List.of(
                new TestdataStringLengthShadowEntity(1L),
                new TestdataStringLengthShadowEntity(2L)));
        planningProblem.setValueList(List.of(new TestdataListValueShadowEntity("a"), new TestdataListValueShadowEntity("bb"),
                new TestdataListValueShadowEntity("ccc")));
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(planningProblem);
        try {
            return benchmark.benchmark().toPath().toAbsolutePath().toString();
        } catch (PlannerBenchmarkException e) {
            // ignore the exception
            return ((DefaultPlannerBenchmark) benchmark).getBenchmarkDirectory().getAbsolutePath();
        }
    }
}
