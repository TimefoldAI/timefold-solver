package ai.timefold.solver.benchmark.quarkus;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.quarkus.config.TimefoldBenchmarkRuntimeConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldBenchmarkRecorder {

    private final RuntimeValue<TimefoldBenchmarkRuntimeConfig> runtimeConfig;

    public TimefoldBenchmarkRecorder(RuntimeValue<TimefoldBenchmarkRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Supplier<PlannerBenchmarkConfig> benchmarkConfigSupplier(PlannerBenchmarkConfig benchmarkConfig) {
        return () -> {
            var solverConfig =
                    Arc.container().instance(SolverConfig.class).get();
            var timefoldRuntimeConfig = runtimeConfig != null ? runtimeConfig.getValue() : null;
            // If the termination configuration is set and the created benchmark configuration has no configuration item,
            // we need to add at least one configuration; otherwise, we will fail to recognize the runtime termination setting.
            if (benchmarkConfig != null && benchmarkConfig.getSolverBenchmarkConfigList() == null &&
                    timefoldRuntimeConfig != null && timefoldRuntimeConfig.termination() != null) {
                benchmarkConfig.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
            }
            return updateBenchmarkConfigWithRuntimeProperties(benchmarkConfig, solverConfig);
        };
    }

    private PlannerBenchmarkConfig updateBenchmarkConfigWithRuntimeProperties(PlannerBenchmarkConfig plannerBenchmarkConfig,
            SolverConfig solverConfig) {
        if (plannerBenchmarkConfig == null) { // no benchmarkConfig.xml provided
            // Can't do this in processor; SolverConfig is not completed yet (has some runtime properties)
            plannerBenchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
        }

        var benchmarkRuntimeConfig = runtimeConfig != null ? runtimeConfig.getValue() : null;
        if (benchmarkRuntimeConfig != null && benchmarkRuntimeConfig.resultDirectory() != null) {
            plannerBenchmarkConfig.setBenchmarkDirectory(new File(benchmarkRuntimeConfig.resultDirectory()));
        }
        var inheritedBenchmarkConfig = plannerBenchmarkConfig.getInheritedSolverBenchmarkConfig();

        if (plannerBenchmarkConfig.getSolverBenchmarkBluePrintConfigList() != null) {
            if (inheritedBenchmarkConfig == null) {
                inheritedBenchmarkConfig = new SolverBenchmarkConfig();
                plannerBenchmarkConfig.setInheritedSolverBenchmarkConfig(inheritedBenchmarkConfig);
                inheritedBenchmarkConfig.setSolverConfig(solverConfig.copyConfig());
            }
            TerminationConfig inheritedTerminationConfig;
            if (inheritedBenchmarkConfig.getSolverConfig().getTerminationConfig() != null) {
                inheritedTerminationConfig = inheritedBenchmarkConfig.getSolverConfig().getTerminationConfig();
            } else {
                inheritedTerminationConfig = new TerminationConfig();
                inheritedBenchmarkConfig.getSolverConfig().setTerminationConfig(inheritedTerminationConfig);
            }
            if (benchmarkRuntimeConfig != null && benchmarkRuntimeConfig.termination() != null) {
                benchmarkRuntimeConfig.termination().spentLimit().ifPresent(inheritedTerminationConfig::setSpentLimit);
                benchmarkRuntimeConfig.termination().unimprovedSpentLimit()
                        .ifPresent(inheritedTerminationConfig::setUnimprovedSpentLimit);
                benchmarkRuntimeConfig.termination().bestScoreLimit().ifPresent(inheritedTerminationConfig::setBestScoreLimit);
            }
        }

        TerminationConfig inheritedTerminationConfig = null;
        if (inheritedBenchmarkConfig != null && inheritedBenchmarkConfig.getSolverConfig() != null &&
                inheritedBenchmarkConfig.getSolverConfig().getTerminationConfig() != null) {
            inheritedTerminationConfig = inheritedBenchmarkConfig.getSolverConfig().getTerminationConfig();
        }

        if (inheritedTerminationConfig == null || !inheritedTerminationConfig.isConfigured()) {
            var solverBenchmarkConfigList = plannerBenchmarkConfig.getSolverBenchmarkConfigList();
            if (solverBenchmarkConfigList == null) {
                throw new IllegalStateException("At least one of the properties " +
                        "quarkus.timefold.benchmark.solver.termination.spent-limit, " +
                        "quarkus.timefold.benchmark.solver.termination.best-score-limit, " +
                        "quarkus.timefold.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in the " +
                        "inherited solver benchmark config and solverBenchmarkBluePrint is used.");
            }
            for (var solverBenchmarkConfig : solverBenchmarkConfigList) {
                var solverConfig_ = Objects.requireNonNullElseGet(solverBenchmarkConfig.getSolverConfig(), SolverConfig::new);
                solverBenchmarkConfig.setSolverConfig(solverConfig_); // In case it was null before.
                var terminationConfig = solverConfig_.getTerminationConfig();
                if (terminationConfig == null) {
                    terminationConfig = new TerminationConfig();
                    solverConfig_.setTerminationConfig(terminationConfig);
                } else if (terminationConfig.isConfigured()) {
                    continue;
                }

                if (benchmarkRuntimeConfig != null && benchmarkRuntimeConfig.termination() != null) {
                    benchmarkRuntimeConfig.termination().spentLimit().ifPresent(terminationConfig::setSpentLimit);
                    benchmarkRuntimeConfig.termination().unimprovedSpentLimit()
                            .ifPresent(terminationConfig::setUnimprovedSpentLimit);
                    benchmarkRuntimeConfig.termination().bestScoreLimit().ifPresent(terminationConfig::setBestScoreLimit);
                }
                if (!terminationConfig.isConfigured() && !solverConfig_.canTerminate()) {
                    throw new IllegalStateException("At least one of the solver benchmarks is not configured to terminate. " +
                            "At least one of the properties " +
                            "quarkus.timefold.benchmark.solver.termination.spent-limit, " +
                            "quarkus.timefold.benchmark.solver.termination.best-score-limit, " +
                            "quarkus.timefold.benchmark.solver.termination.unimproved-spent-limit " +
                            "is required if termination is not configured in a solver benchmark and the " +
                            "inherited solver benchmark config.");
                }
            }
        }

        if (plannerBenchmarkConfig.getSolverBenchmarkConfigList() != null) {
            for (var childBenchmarkConfig : plannerBenchmarkConfig.getSolverBenchmarkConfigList()) {
                if (childBenchmarkConfig.getSolverConfig() == null) {
                    childBenchmarkConfig.setSolverConfig(new SolverConfig());
                }
                inheritPropertiesFromSolverConfig(childBenchmarkConfig, inheritedBenchmarkConfig, solverConfig);
            }
        }

        if (plannerBenchmarkConfig.getSolverBenchmarkConfigList() == null
                && plannerBenchmarkConfig.getSolverBenchmarkBluePrintConfigList() == null) {
            plannerBenchmarkConfig.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        }
        return plannerBenchmarkConfig;
    }

    private void inheritPropertiesFromSolverConfig(SolverBenchmarkConfig childBenchmarkConfig,
            SolverBenchmarkConfig inheritedBenchmarkConfig,
            SolverConfig solverConfig) {
        inheritProperty(childBenchmarkConfig, inheritedBenchmarkConfig, solverConfig,
                SolverConfig::getSolutionClass, SolverConfig::setSolutionClass);
        inheritProperty(childBenchmarkConfig, inheritedBenchmarkConfig, solverConfig,
                SolverConfig::getEntityClassList, SolverConfig::setEntityClassList);
        inheritScoreCalculation(childBenchmarkConfig, inheritedBenchmarkConfig, solverConfig);
    }

    private <T> void inheritProperty(SolverBenchmarkConfig childBenchmarkConfig,
            SolverBenchmarkConfig inheritedBenchmarkConfig,
            SolverConfig solverConfig,
            Function<SolverConfig, T> getter,
            BiConsumer<SolverConfig, T> setter) {
        if (getter.apply(childBenchmarkConfig.getSolverConfig()) != null) {
            return;
        }
        if (inheritedBenchmarkConfig != null && inheritedBenchmarkConfig.getSolverConfig() != null &&
                getter.apply(inheritedBenchmarkConfig.getSolverConfig()) != null) {
            return;
        }
        setter.accept(childBenchmarkConfig.getSolverConfig(), getter.apply(solverConfig));
    }

    private void inheritScoreCalculation(SolverBenchmarkConfig childBenchmarkConfig,
            SolverBenchmarkConfig inheritedBenchmarkConfig,
            SolverConfig solverConfig) {

        if (isScoreCalculationDefined(childBenchmarkConfig.getSolverConfig())) {
            return;
        }
        if (inheritedBenchmarkConfig != null && inheritedBenchmarkConfig.getSolverConfig() != null &&
                isScoreCalculationDefined(inheritedBenchmarkConfig.getSolverConfig())) {
            return;
        }
        var childScoreDirectorFactoryConfig = Objects.requireNonNull(childBenchmarkConfig.getSolverConfig())
                .getScoreDirectorFactoryConfig();
        var inheritedScoreDirectorFactoryConfig = Objects.requireNonNull(solverConfig.getScoreDirectorFactoryConfig());
        if (childScoreDirectorFactoryConfig == null) {
            childScoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
            Objects.requireNonNull(childBenchmarkConfig.getSolverConfig())
                    .setScoreDirectorFactoryConfig(childScoreDirectorFactoryConfig);
        }
        childScoreDirectorFactoryConfig.inherit(inheritedScoreDirectorFactoryConfig);
    }

    private boolean isScoreCalculationDefined(SolverConfig solverConfig) {
        if (solverConfig == null) {
            return false;
        }
        var scoreDirectorFactoryConfig = solverConfig.getScoreDirectorFactoryConfig();
        if (scoreDirectorFactoryConfig == null) {
            return false;
        }
        return scoreDirectorFactoryConfig.getEasyScoreCalculatorClass() != null ||
                scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass() != null ||
                scoreDirectorFactoryConfig.getConstraintProviderClass() != null;
    }
}
