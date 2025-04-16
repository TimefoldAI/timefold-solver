package ai.timefold.solver.core.preview.api.variable.declarative.fsr;

import static ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVisit.BASE_START_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ChangedVariableNotifier;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRAssertionEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRRoutePlan;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVehicle;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVisit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FSRShadowVariableTest {
    public static class MockShadowVariableSession<Solution_> {
        final SolutionDescriptor<Solution_> solutionDescriptor;
        final VariableReferenceGraph<Solution_> graph;

        public MockShadowVariableSession(SolutionDescriptor<Solution_> solutionDescriptor,
                VariableReferenceGraph<Solution_> graph) {
            this.solutionDescriptor = solutionDescriptor;
            this.graph = graph;
        }

        public void setVariable(Object entity, String variableName, @Nullable Object value) {
            var variableMetamodel = solutionDescriptor.getMetaModel().entity(entity.getClass()).variable(variableName);
            graph.beforeVariableChanged(variableMetamodel, entity);
            solutionDescriptor.getEntityDescriptorStrict(entity.getClass()).getVariableDescriptor(variableName)
                    .setValue(entity, value);
            graph.afterVariableChanged(variableMetamodel, entity);
        }

        public void updateVariables() {
            graph.updateChanged();
        }
    }

    private <Solution_> MockShadowVariableSession<Solution_> createSession(SolutionDescriptor<Solution_> solutionDescriptor,
            Object... entities) {
        var variableReferenceGraph = new VariableReferenceGraph<Solution_>(ChangedVariableNotifier.empty());

        DefaultShadowVariableSessionFactory.visitGraph(solutionDescriptor, variableReferenceGraph, entities,
                DefaultTopologicalOrderGraph::new);

        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph);
    }

    @Test
    public void simpleChain() {
        var vehicle = spy(new TestdataFSRVehicle("v1"));
        var visit1 = spy(new TestdataFSRVisit("c1"));
        var visit2 = spy(new TestdataFSRVisit("c2"));
        var visit3 = spy(new TestdataFSRVisit("c3"));

        var session = createSession(
                TestdataFSRRoutePlan.buildSolutionDescriptor(),
                vehicle, visit1, visit2, visit3);
        session.setVariable(visit1, "vehicle", vehicle);
        session.setVariable(visit2, "previousVisit", visit1);
        session.setVariable(visit3, "previousVisit", visit2);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        assertThat(visit1.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visit1.isInvalid()).isFalse();

        assertThat(visit2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visit2.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visit2.isInvalid()).isFalse();

        assertThat(visit3.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visit3.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visit3.isInvalid()).isFalse();

        session.setVariable(visit2, "previousVisit", visit3);
        session.setVariable(visit3, "previousVisit", visit1);

        Mockito.reset(vehicle, visit1, visit2, visit3);
        session.updateVariables();

        verifyNoInteractions(visit1);

        assertThat(visit1.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visit1.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visit1.isInvalid()).isFalse();

        assertThat(visit3.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visit3.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visit2.isInvalid()).isFalse();

        assertThat(visit2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visit2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visit3.isInvalid()).isFalse();
    }

    @Test
    public void groupChain() {
        var vehicle1 = new TestdataFSRVehicle("v1");
        var vehicle2 = new TestdataFSRVehicle("v2");
        var vehicle3 = new TestdataFSRVehicle("v3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");
        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");
        var visitC = new TestdataFSRVisit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session = createSession(TestdataFSRRoutePlan.buildSolutionDescriptor(),
                vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setVariable(visitA1, "vehicle", vehicle1);
        session.setVariable(visitA2, "vehicle", vehicle2);
        session.setVariable(visitB1, "vehicle", vehicle3);
        session.setVariable(visitB2, "previousVisit", visitA1);
        session.setVariable(visitB3, "previousVisit", visitA2);
        session.setVariable(visitC, "previousVisit", visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();

        session.setVariable(visitC, "previousVisit", null);
        session.setVariable(visitC, "vehicle", vehicle1);
        session.setVariable(visitA1, "previousVisit", visitC);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitA2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceStartTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceStartTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceStartTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitB3.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitC.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    public void groupChainValidToInvalid() {
        var vehicle1 = new TestdataFSRVehicle("v1");
        var vehicle2 = new TestdataFSRVehicle("v2");
        var vehicle3 = new TestdataFSRVehicle("v3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");
        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");
        var visitC = new TestdataFSRVisit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session = createSession(TestdataFSRRoutePlan.buildSolutionDescriptor(),
                vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setVariable(visitA1, "vehicle", vehicle1);
        session.setVariable(visitA2, "vehicle", vehicle2);
        session.setVariable(visitB1, "vehicle", vehicle3);
        session.setVariable(visitB2, "previousVisit", visitA1);
        session.setVariable(visitB3, "previousVisit", visitA2);
        session.setVariable(visitC, "previousVisit", visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();

        session.setVariable(visitA1, "previousVisit", visitB2);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceStartTime()).isNull();
        assertThat(visitC.getServiceFinishTime()).isNull();
        assertThat(visitC.isInvalid()).isTrue();

        session.setVariable(visitC, "previousVisit", null);
        session.setVariable(visitC, "vehicle", vehicle2);
        session.setVariable(visitA2, "previousVisit", visitC);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitC.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    public void groupChainInvalidToValid() {
        var vehicle1 = new TestdataFSRVehicle("v1");
        var vehicle2 = new TestdataFSRVehicle("v2");
        var vehicle3 = new TestdataFSRVehicle("v3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");
        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");
        var visitC = new TestdataFSRVisit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        var session = createSession(TestdataFSRRoutePlan.buildSolutionDescriptor(),
                vehicle1, vehicle2, vehicle3, visitA1, visitA2, visitB1, visitB2, visitB3, visitC);
        session.setVariable(visitA1, "vehicle", vehicle1);
        session.setVariable(visitA2, "vehicle", vehicle2);
        session.setVariable(visitB1, "vehicle", vehicle1);
        session.setVariable(visitB2, "previousVisit", visitA1);
        session.setVariable(visitB3, "previousVisit", visitA2);
        session.setVariable(visitA1, "previousVisit", visitB1);
        session.setVariable(visitC, "previousVisit", visitB1);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isNull();
        assertThat(visitA1.getServiceFinishTime()).isNull();
        assertThat(visitA1.isInvalid()).isTrue();

        assertThat(visitA2.getServiceStartTime()).isNull();
        assertThat(visitA2.getServiceFinishTime()).isNull();
        assertThat(visitA2.isInvalid()).isTrue();

        assertThat(visitB1.getServiceStartTime()).isNull();
        assertThat(visitB1.getServiceFinishTime()).isNull();
        assertThat(visitB1.isInvalid()).isTrue();

        assertThat(visitB2.getServiceStartTime()).isNull();
        assertThat(visitB2.getServiceFinishTime()).isNull();
        assertThat(visitB2.isInvalid()).isTrue();

        assertThat(visitB3.getServiceStartTime()).isNull();
        assertThat(visitB3.getServiceFinishTime()).isNull();
        assertThat(visitB3.isInvalid()).isTrue();

        assertThat(visitC.getServiceStartTime()).isNull();
        assertThat(visitC.getServiceFinishTime()).isNull();
        assertThat(visitC.isInvalid()).isTrue();

        session.setVariable(visitA1, "previousVisit", null);
        session.setVariable(visitB1, "vehicle", vehicle3);

        session.updateVariables();

        assertThat(visitA1.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA1.isInvalid()).isFalse();

        assertThat(visitA2.getServiceStartTime()).isEqualTo(BASE_START_TIME);
        assertThat(visitA2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(30L));
        assertThat(visitA2.isInvalid()).isFalse();

        assertThat(visitB1.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB1.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB1.isInvalid()).isFalse();

        assertThat(visitB2.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB2.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB2.isInvalid()).isFalse();

        assertThat(visitB3.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(60L));
        assertThat(visitB3.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(90L));
        assertThat(visitB3.isInvalid()).isFalse();

        assertThat(visitC.getServiceStartTime()).isEqualTo(BASE_START_TIME.plusMinutes(120L));
        assertThat(visitC.getServiceFinishTime())
                .isEqualTo(BASE_START_TIME.plusMinutes(150L));
        assertThat(visitC.isInvalid()).isFalse();
    }

    @Test
    void solveNoVisitGroups() {
        var problem = new TestdataFSRRoutePlan();
        var vehicle1 = new TestdataFSRVehicle("v1");
        var vehicle2 = new TestdataFSRVehicle("v2");
        var vehicle3 = new TestdataFSRVehicle("v3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");
        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");
        var visitC = new TestdataFSRVisit("c");

        problem.setVehicles(List.of(vehicle1, vehicle2, vehicle3));
        problem.setVisits(List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC));

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withSolutionClass(TestdataFSRRoutePlan.class)
                .withEntityClasses(TestdataFSRVehicle.class, TestdataFSRVisit.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataFSRConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withConstraintProviderClass(TestdataFSRConstraintProvider.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        solver.solve(problem);
    }

    @Test
    void solveVisitGroups() {
        var problem = new TestdataFSRRoutePlan();
        var vehicle1 = new TestdataFSRVehicle("v1");
        var vehicle2 = new TestdataFSRVehicle("v2");
        var vehicle3 = new TestdataFSRVehicle("v3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");
        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");
        var visitC = new TestdataFSRVisit("c");

        var visitGroupA = List.of(visitA1, visitA2);
        var visitGroupB = List.of(visitB1, visitB2, visitB3);

        visitA1.setVisitGroup(visitGroupA);
        visitA2.setVisitGroup(visitGroupA);

        visitB1.setVisitGroup(visitGroupB);
        visitB2.setVisitGroup(visitGroupB);
        visitB3.setVisitGroup(visitGroupB);

        problem.setVehicles(List.of(vehicle1, vehicle2, vehicle3));
        problem.setVisits(List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC));

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withSolutionClass(TestdataFSRRoutePlan.class)
                .withEntityClasses(TestdataFSRVehicle.class, TestdataFSRVisit.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataFSRConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withEasyScoreCalculatorClass(TestdataFSRAssertionEasyScoreCalculator.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        solver.solve(problem);
    }
}
