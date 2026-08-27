package ai.timefold.solver.benchmark.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.junit.jupiter.api.Test;

class DefaultPlannerBenchmarkFactoryTest {

    @Test
    void validNameWithUnderscoreAndSpace() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setName("Valid_name with space_and_underscore");
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        new DefaultPlannerBenchmarkFactory(config).validate();
    }

    @Test
    void validNameWithJapanese() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setName("Valid name (有効名 in Japanese)");
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        benchmarkFactory.validate();
    }

    @Test
    void invalidNameWithSlash() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setName("slash/name");
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::validate);
    }

    @Test
    void invalidNameWithSuffixWhitespace() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setName("Suffixed with space ");
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::validate);
    }

    @Test
    void invalidNameWithPrefixWhitespace() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setName(" prefixed with space");
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::validate);
    }

    @Test
    void noSolverConfigs() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setSolverBenchmarkConfigList(null);
        config.setSolverBenchmarkBluePrintConfigList(null);
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalArgumentException().isThrownBy(benchmarkFactory::validate);
    }

    @Test
    void phaseWithoutEnvironmentModeIsValid() {
        PlannerBenchmarkConfig config = buildConfigWithPhases(EnvironmentMode.FULL_ASSERT, null, null);
        new DefaultPlannerBenchmarkFactory(config).validate();
    }

    @Test
    void phaseOverridingEnvironmentModeIsRejected() {
        // The scenario this guards: a leftover FULL_ASSERT on one phase handicaps this solver benchmark
        // against the others, while the report still states one environment mode for the whole run.
        PlannerBenchmarkConfig config = buildConfigWithPhases(EnvironmentMode.PHASE_ASSERT,
                null, EnvironmentMode.FULL_ASSERT);
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::validate)
                .withMessageContaining("cannot override the environment mode when in benchmark mode");
    }

    @Test
    void anyEnvironmentModeOverrideIsRejected() {
        // The offending solver benchmark is the last one, so every one of them has to be checked.
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setSolverBenchmarkConfigList(Arrays.asList(
                buildSolverBenchmarkConfig(EnvironmentMode.PHASE_ASSERT, null, null),
                buildSolverBenchmarkConfig(EnvironmentMode.PHASE_ASSERT, null, EnvironmentMode.FULL_ASSERT)));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::validate)
                .withMessageContaining("cannot override the environment mode when in benchmark mode");
    }

    @Test
    void solverBenchmarkWithoutSolverConfigIsValid() {
        // A <solverBenchmark> without a <solver> has no phases, so it has nothing to override.
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        new DefaultPlannerBenchmarkFactory(config).validate();
    }

    private static PlannerBenchmarkConfig buildConfigWithPhases(EnvironmentMode globalEnvironmentMode,
            EnvironmentMode constructionHeuristicEnvironmentMode, EnvironmentMode localSearchEnvironmentMode) {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setSolverBenchmarkConfigList(Collections.singletonList(buildSolverBenchmarkConfig(globalEnvironmentMode,
                constructionHeuristicEnvironmentMode, localSearchEnvironmentMode)));
        return config;
    }

    private static SolverBenchmarkConfig buildSolverBenchmarkConfig(EnvironmentMode globalEnvironmentMode,
            EnvironmentMode constructionHeuristicEnvironmentMode, EnvironmentMode localSearchEnvironmentMode) {
        ConstructionHeuristicPhaseConfig constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        constructionHeuristicPhaseConfig.setEnvironmentMode(constructionHeuristicEnvironmentMode);
        LocalSearchPhaseConfig localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig.setEnvironmentMode(localSearchEnvironmentMode);
        SolverConfig solverConfig = new SolverConfig()
                .withEnvironmentMode(globalEnvironmentMode)
                .withPhases(constructionHeuristicPhaseConfig, localSearchPhaseConfig);
        SolverBenchmarkConfig solverBenchmarkConfig = new SolverBenchmarkConfig();
        solverBenchmarkConfig.setSolverConfig(solverConfig);
        return solverBenchmarkConfig;
    }

    @Test
    void nonUniqueSolverConfigName() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        final String sbcName = "x";
        SolverBenchmarkConfig sbc1 = new SolverBenchmarkConfig();
        sbc1.setName(sbcName);
        SolverBenchmarkConfig sbc2 = new SolverBenchmarkConfig();
        sbc2.setName(sbcName);
        config.setSolverBenchmarkConfigList(Arrays.asList(sbc1, sbc2));
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalStateException().isThrownBy(benchmarkFactory::generateSolverBenchmarkConfigNames);
    }

    @Test
    void uniqueNamesGenerated() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        SolverBenchmarkConfig sbc1 = new SolverBenchmarkConfig();
        SolverBenchmarkConfig sbc2 = new SolverBenchmarkConfig();
        SolverBenchmarkConfig sbc3 = new SolverBenchmarkConfig();
        sbc3.setName("Config_1");
        List<SolverBenchmarkConfig> configs = Arrays.asList(sbc1, sbc2, sbc3);
        config.setSolverBenchmarkConfigList(configs);
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        benchmarkFactory.generateSolverBenchmarkConfigNames();
        assertThat(sbc3.getName()).isEqualTo("Config_1");
        TreeSet<String> names = new TreeSet<>();
        for (SolverBenchmarkConfig sc : configs) {
            names.add(sc.getName());
        }
        for (int i = 0; i < configs.size(); i++) {
            assertThat(names).contains("Config_" + i);
        }
    }

    @Test
    void resolveParallelBenchmarkCountAutomatically() {
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(new PlannerBenchmarkConfig());
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(-1)).isEqualTo(1);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(0)).isEqualTo(1);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(1)).isEqualTo(1);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(2)).isEqualTo(1);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(3)).isEqualTo(2);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(4)).isEqualTo(2);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(5)).isEqualTo(3);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(6)).isEqualTo(4);
        assertThat(benchmarkFactory.resolveParallelBenchmarkCountAutomatically(17)).isEqualTo(9);
    }

    @Test
    void parallelBenchmarkDisabledByDefault() {
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(new PlannerBenchmarkConfig());
        assertThat(benchmarkFactory.resolveParallelBenchmarkCount()).isEqualTo(1);
    }

    @Test
    void resolvedParallelBenchmarkCountNegative() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setParallelBenchmarkCount("-1");
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThatIllegalArgumentException().isThrownBy(benchmarkFactory::resolveParallelBenchmarkCount);
    }

    @Test
    void calculateWarmUpTimeMillisSpentLimit() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setWarmUpHoursSpentLimit(1L);
        config.setWarmUpMinutesSpentLimit(2L);
        config.setWarmUpSecondsSpentLimit(5L);
        config.setWarmUpMillisecondsSpentLimit(753L);
        DefaultPlannerBenchmarkFactory benchmarkFactory = new DefaultPlannerBenchmarkFactory(config);
        assertThat(benchmarkFactory.calculateWarmUpTimeMillisSpentLimit(30_000L))
                .isEqualTo(3_725_753L);
    }
}
