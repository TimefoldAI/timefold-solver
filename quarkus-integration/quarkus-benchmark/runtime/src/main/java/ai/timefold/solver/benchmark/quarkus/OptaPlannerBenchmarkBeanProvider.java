package ai.timefold.solver.benchmark.quarkus;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;

import io.quarkus.arc.DefaultBean;

public class OptaPlannerBenchmarkBeanProvider {

    @DefaultBean
    @Singleton
    @Produces
    PlannerBenchmarkFactory benchmarkFactory(PlannerBenchmarkConfig plannerBenchmarkConfig) {
        return PlannerBenchmarkFactory.create(plannerBenchmarkConfig);
    }

}
