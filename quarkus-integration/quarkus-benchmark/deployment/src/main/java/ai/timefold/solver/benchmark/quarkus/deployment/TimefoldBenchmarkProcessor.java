package ai.timefold.solver.benchmark.quarkus.deployment;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.TimefoldBenchmarkBeanProvider;
import ai.timefold.solver.benchmark.quarkus.TimefoldBenchmarkRecorder;
import ai.timefold.solver.benchmark.quarkus.UnavailableTimefoldBenchmarkBeanProvider;
import ai.timefold.solver.benchmark.quarkus.config.TimefoldBenchmarkRuntimeConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.deployment.SolverConfigBuildItem;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

class TimefoldBenchmarkProcessor {

    private static final Logger log = Logger.getLogger(TimefoldBenchmarkProcessor.class.getName());

    TimefoldBenchmarkBuildTimeConfig timefoldBenchmarkBuildTimeConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold-solver-benchmark");
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchSolverBenchmarkConfigXml() {
        String solverBenchmarkConfigXML = timefoldBenchmarkBuildTimeConfig.solverBenchmarkConfigXml
                .orElse(TimefoldBenchmarkBuildTimeConfig.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL);
        return new HotDeploymentWatchedFileBuildItem(solverBenchmarkConfigXML);
    }

    @BuildStep
    BenchmarkConfigBuildItem registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            SolverConfigBuildItem solverConfigBuildItem) {
        // We don't support benchmarking for multiple solvers
        if (solverConfigBuildItem.getSolverConfigMap().size() > 1) {
            throw new ConfigurationException("""
                    When defining multiple solvers, the benchmark feature is not enabled.
                    Consider using separate <solverBenchmark> instances for evaluating different solver configurations.""");
        }
        if (solverConfigBuildItem.getGeneratedGizmoClasses() == null) {
            log.warn("Skipping Timefold Benchmark extension because the Timefold extension was skipped.");
            additionalBeans.produce(new AdditionalBeanBuildItem(UnavailableTimefoldBenchmarkBeanProvider.class));
            return new BenchmarkConfigBuildItem(null);
        }
        PlannerBenchmarkConfig benchmarkConfig;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (timefoldBenchmarkBuildTimeConfig.solverBenchmarkConfigXml.isPresent()) {
            String solverBenchmarkConfigXML = timefoldBenchmarkBuildTimeConfig.solverBenchmarkConfigXml.get();
            if (classLoader.getResource(solverBenchmarkConfigXML) == null) {
                throw new ConfigurationException("Invalid quarkus.timefold.benchmark.solver-benchmark-config-xml property ("
                        + solverBenchmarkConfigXML + "): that classpath resource does not exist.");
            }
            benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(solverBenchmarkConfigXML);
        } else if (classLoader.getResource(TimefoldBenchmarkBuildTimeConfig.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL) != null) {
            benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                    TimefoldBenchmarkBuildTimeConfig.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL);
        } else {
            benchmarkConfig = null;
        }
        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldBenchmarkBeanProvider.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldBenchmarkRuntimeConfig.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(SolverConfig.class));
        return new BenchmarkConfigBuildItem(benchmarkConfig);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerRuntimeBeans(TimefoldBenchmarkRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            SolverConfigBuildItem solverConfigBuildItem, BenchmarkConfigBuildItem benchmarkConfigBuildItem,
            TimefoldBenchmarkRuntimeConfig runtimeConfig) {
        if (solverConfigBuildItem.getGeneratedGizmoClasses() == null) {
            return;
        }
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(PlannerBenchmarkConfig.class)
                .supplier(recorder.benchmarkConfigSupplier(benchmarkConfigBuildItem.getBenchmarkConfig(), runtimeConfig))
                .setRuntimeInit()
                .done());
    }
}
