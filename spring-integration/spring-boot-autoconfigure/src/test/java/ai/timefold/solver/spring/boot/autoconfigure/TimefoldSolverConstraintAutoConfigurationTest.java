package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverProperty;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.multiple.MultipleConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NativeDetector;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverConstraintAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner multiConstraintProviderRunner;
    private final ApplicationContextRunner fakeNativeWithNodeSharingContextRunner;
    private final ApplicationContextRunner fakeNativeWithoutNodeSharingContextRunner;
    private final FilteredClassLoader testFilteredClassLoader;

    public TimefoldSolverConstraintAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        multiConstraintProviderRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleConstraintSpringTestConfiguration.class);
        fakeNativeWithNodeSharingContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.%s=true"
                        .formatted(SolverProperty.CONSTRAINT_STREAM_AUTOMATIC_NODE_SHARING.getPropertyName()));
        fakeNativeWithoutNodeSharingContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.%s=false"
                        .formatted(SolverProperty.CONSTRAINT_STREAM_AUTOMATIC_NODE_SHARING.getPropertyName()));
        testFilteredClassLoader =
                new FilteredClassLoader(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL));
    }

    @Test
    void constraintMetaModel() {
        contextRunner.withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    var constraintMetaModel = context.getBean(ConstraintMetaModel.class);
                    assertThat(constraintMetaModel).isNotNull();
                });
    }

    @Test
    void constraintVerifier() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);

                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

                    problem.getEntityList().get(0).setValue("v1");
                    problem.getEntityList().get(1).setValue("v1");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(-2));

                    problem.getEntityList().get(1).setValue("v2");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(0));
                });
    }

    @Test
    void constraintVerifierBavet() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/bavetSolverConfig.xml")
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);

                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

                    problem.getEntityList().get(0).setValue("v1");
                    problem.getEntityList().get(1).setValue("v1");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(-2));

                    problem.getEntityList().get(1).setValue("v2");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(0));
                });
    }

    @Test
    void readOnlyConcreteProviderClass() {
        AssertionsForClassTypes.assertThatCode(() -> multiConstraintProviderRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void nodeSharingFailFastInNativeImage() {
        try (var nativeDetectorMock = Mockito.mockStatic(NativeDetector.class)) {
            nativeDetectorMock.when(NativeDetector::inNativeImage).thenReturn(true);
            fakeNativeWithNodeSharingContextRunner
                    .run(context -> {
                        Throwable startupFailure = context.getStartupFailure();
                        assertThat(startupFailure)
                                .isInstanceOf(UnsupportedOperationException.class)
                                .hasMessageContainingAll("node sharing", "unsupported", "native");
                    });
        }

    }

    @Test
    void nodeSharingDisabledWorksInNativeImage() {
        try (var nativeDetectorMock = Mockito.mockStatic(NativeDetector.class)) {
            nativeDetectorMock.when(NativeDetector::inNativeImage).thenReturn(true);
            fakeNativeWithoutNodeSharingContextRunner
                    .run(context -> {
                        var solverConfig = context.getBean(SolverConfig.class);
                        assertThat(solverConfig).isNotNull();
                        assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                        assertThat(solverConfig.getEntityClassList())
                                .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                                .isEqualTo(TestdataSpringConstraintProvider.class);
                        // Properties defined in solverConfig.xml
                        assertThat(solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue()).isEqualTo(2L);
                        var solverFactory = context.getBean(SolverFactory.class);
                        assertThat(solverFactory).isNotNull();
                        assertThat(solverFactory.buildSolver()).isNotNull();
                    });
        }
    }

}
