package ai.timefold.solver.core.preview.api.variable.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ProvidedShadowVariableTest {
    @Test
    public void simpleChain() {
        var sessionFactory = ShadowVariableSessionFactory.create(
                SolutionDescriptor.buildSolutionDescriptor(RoutePlan.class,
                        Vehicle.class, Visit.class),
                new TestShadowVariableProvider());

        var vehicle = spy(new Vehicle("v1"));
        var visit1 = spy(new Visit("c1"));
        var visit2 = spy(new Visit("c2"));
        var visit3 = spy(new Visit("c3"));

        var session = sessionFactory.forEntities(vehicle, visit1, visit2, visit3);
        session.setInverse(visit1, vehicle);
        session.setPrevious(visit2, visit1);
        session.setPrevious(visit3, visit2);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        assertThat(visit1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visit1.isInvalid()).isFalse();

        assertThat(visit2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visit2.isInvalid()).isFalse();

        assertThat(visit3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visit3.isInvalid()).isFalse();

        session.setPrevious(visit2, visit3);
        session.setPrevious(visit3, visit1);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        verifyNoInteractions(visit1);

        assertThat(visit1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visit1.isInvalid()).isFalse();

        assertThat(visit3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visit3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visit2.isInvalid()).isFalse();

        assertThat(visit2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visit2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visit3.isInvalid()).isFalse();
    }

    @Test
    public void groupChain() {
        var sessionFactory = ShadowVariableSessionFactory.create(
                SolutionDescriptor.buildSolutionDescriptor(RoutePlan.class,
                        Vehicle.class, Visit.class),
                new TestShadowVariableProvider());

        var vehicle1 = new Vehicle("v1");
        var vehicle2 = new Vehicle("v2");
        var vehicle3 = new Vehicle("v3");

        var visitA1 = new Visit("a1");
        var visitA2 = new Visit("a2");
        var visitB1 = new Visit("b1");
        var visitB2 = new Visit("b2");
        var visitB3 = new Visit("b3");
        var visitC = new Visit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session =
                sessionFactory.forEntities(vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setInverse(visitA1, vehicle1);
        session.setInverse(visitA2, vehicle2);
        session.setInverse(visitB1, vehicle3);
        session.setPrevious(visitB2, visitA1);
        session.setPrevious(visitB3, visitA2);
        session.setPrevious(visitC, visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();

        session.setPrevious(visitC, null);
        session.setInverse(visitC, vehicle1);
        session.setPrevious(visitA1, visitC);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitC.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitC.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    public void groupChainValidToInvalid() {
        var sessionFactory = ShadowVariableSessionFactory.create(
                SolutionDescriptor.buildSolutionDescriptor(RoutePlan.class,
                        Vehicle.class, Visit.class),
                new TestShadowVariableProvider());

        var vehicle1 = new Vehicle("v1");
        var vehicle2 = new Vehicle("v2");
        var vehicle3 = new Vehicle("v3");

        var visitA1 = new Visit("a1");
        var visitA2 = new Visit("a2");
        var visitB1 = new Visit("b1");
        var visitB2 = new Visit("b2");
        var visitB3 = new Visit("b3");
        var visitC = new Visit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session =
                sessionFactory.forEntities(vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setInverse(visitA1, vehicle1);
        session.setInverse(visitA2, vehicle2);
        session.setInverse(visitB1, vehicle3);
        session.setPrevious(visitB2, visitA1);
        session.setPrevious(visitB3, visitA2);
        session.setPrevious(visitC, visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();

        session.setPrevious(visitA1, visitB2);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isNull();
        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceReadyTime()).isNull();
        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceReadyTime()).isNull();
        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceReadyTime()).isNull();
        assertThat(visitC.getServiceStartTime()).isNull();
        assertThat(visitC.getServiceFinishTime()).isNull();
        assertThat(visitC.isInvalid()).isTrue();

        session.setPrevious(visitC, null);
        session.setInverse(visitC, vehicle2);
        session.setPrevious(visitA2, visitC);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isNull();
        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceReadyTime()).isNull();
        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceReadyTime()).isNull();
        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitC.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitC.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    public void groupChainInvalidToValid() {
        var sessionFactory = ShadowVariableSessionFactory.create(
                SolutionDescriptor.buildSolutionDescriptor(RoutePlan.class,
                        Vehicle.class, Visit.class),
                new TestShadowVariableProvider());

        var vehicle1 = new Vehicle("v1");
        var vehicle2 = new Vehicle("v2");
        var vehicle3 = new Vehicle("v3");

        var visitA1 = new Visit("a1");
        var visitA2 = new Visit("a2");
        var visitB1 = new Visit("b1");
        var visitB2 = new Visit("b2");
        var visitB3 = new Visit("b3");
        var visitC = new Visit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session =
                sessionFactory.forEntities(vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setInverse(visitA1, vehicle1);
        session.setInverse(visitA2, vehicle2);
        session.setInverse(visitB1, vehicle1);
        session.setPrevious(visitB2, visitA1);
        session.setPrevious(visitB3, visitA2);
        session.setPrevious(visitA1, visitB1);
        session.setPrevious(visitC, visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isNull();
        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceReadyTime()).isNull();
        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceReadyTime()).isNull();
        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceReadyTime()).isNull();
        assertThat(visitC.getServiceStartTime()).isNull();
        assertThat(visitC.getServiceFinishTime()).isNull();
        assertThat(visitC.isInvalid()).isTrue();

        session.setPrevious(visitA1, null);
        session.setInverse(visitB1, vehicle3);

        session.updateVariables();

        assertThat(visitA1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME);
        assertThat(visitB1.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceReadyTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceStartTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime()).isEqualTo(TestShadowVariableProvider.BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    void solveNoVisitGroups() {
        var problem = new RoutePlan();
        var vehicle1 = new Vehicle("v1");
        var vehicle2 = new Vehicle("v2");
        var vehicle3 = new Vehicle("v3");

        var visitA1 = new Visit("a1");
        var visitA2 = new Visit("a2");
        var visitB1 = new Visit("b1");
        var visitB2 = new Visit("b2");
        var visitB3 = new Visit("b3");
        var visitC = new Visit("c");

        problem.vehicles = List.of(vehicle1, vehicle2, vehicle3);
        problem.visits = List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC);

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(RoutePlan.class)
                .withEntityClasses(Vehicle.class, Visit.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(RouteConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withConstraintProviderClass(AssertionRouteConstraintProvider.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        solver.solve(problem);
    }

    @Test
    @Disabled // TODO: Fix me; score corruption (although no shadow variable corruption?)
    void solveVisitGroups() {
        var problem = new RoutePlan();
        var vehicle1 = new Vehicle("v1");
        var vehicle2 = new Vehicle("v2");
        var vehicle3 = new Vehicle("v3");

        var visitA1 = new Visit("a1");
        var visitA2 = new Visit("a2");
        var visitB1 = new Visit("b1");
        var visitB2 = new Visit("b2");
        var visitB3 = new Visit("b3");
        var visitC = new Visit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        problem.vehicles = List.of(vehicle1, vehicle2, vehicle3);
        problem.visits = List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC);

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(RoutePlan.class)
                .withEntityClasses(Vehicle.class, Visit.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(RouteConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withConstraintProviderClass(AssertionRouteConstraintProvider.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        solver.solve(problem);
    }
}
