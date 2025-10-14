package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.GizmoSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverGizmoAutoConfigurationTest {

    private final ApplicationContextRunner gizmoContextRunner;
    private final FilteredClassLoader noGizmoFilteredClassLoader;

    public TimefoldSolverGizmoAutoConfigurationTest() {
        gizmoContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(GizmoSpringTestConfiguration.class);
        noGizmoFilteredClassLoader = new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("io.quarkus.gizmo"),
                FilteredClassLoader.ClassPathResourceFilter.of(
                        new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
    }

    @Test
    void solverProperties() {
        gizmoContextRunner
                .withPropertyValues("timefold.solver.domain-access-type=GIZMO")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDomainAccessType()).isEqualTo(DomainAccessType.GIZMO);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        gizmoContextRunner
                .withPropertyValues("timefold.solver.solver1.domain-access-type=GIZMO")
                .withPropertyValues("timefold.solver.solver2.domain-access-type=REFLECTION")
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
    }

    @Test
    void gizmoThrowsIfGizmoNotPresent() {
        assertThatCode(() -> gizmoContextRunner
                .withClassLoader(noGizmoFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/gizmoSpringBootSolverConfig.xml")
                .run(context -> context.getBean(SolverFactory.class)))
                .hasRootCauseMessage("When using the domainAccessType (" +
                        DomainAccessType.GIZMO +
                        ") the classpath or modulepath must contain io.quarkus.gizmo:gizmo.\n" +
                        "Maybe add a dependency to io.quarkus.gizmo:gizmo.");
    }

}
