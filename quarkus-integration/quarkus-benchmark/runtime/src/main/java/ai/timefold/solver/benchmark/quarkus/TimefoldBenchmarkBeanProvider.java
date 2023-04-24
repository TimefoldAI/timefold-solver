package ai.timefold.solver.benchmark.quarkus;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;

import io.quarkus.arc.DefaultBean;

public class TimefoldBenchmarkBeanProvider {

    @DefaultBean
    @Singleton
    @Produces
    PlannerBenchmarkFactory benchmarkFactory(PlannerBenchmarkConfig plannerBenchmarkConfig) {
        return PlannerBenchmarkFactory.create(plannerBenchmarkConfig);
    }

}
