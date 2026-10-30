package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.group_chain.TestdataGroupChainEntity;
import ai.timefold.solver.core.testdomain.shadow.group_chain.TestdataGroupChainSolution;
import ai.timefold.solver.core.testdomain.shadow.group_chain.TestdataGroupChainValue;

import org.junit.jupiter.api.Test;

/**
 * Covers a group that may join the node after a split that stays, and one that must not.
 */
class GroupChainShadowVariableTest {

    private static final OffsetDateTime START = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final List<String> ALL_VARIABLES =
            List.of("readyTime", "startTime", "endTime", "handoverTime", "reportTime");

    @Test
    void groupJoinsTheNodeAfterAKeptSplitOnlyWhenItReadsNothingBeforeThatSplit() {
        var entity1 = new TestdataGroupChainEntity("E1", START);
        var entity2 = new TestdataGroupChainEntity("E2", START.plusMinutes(15));
        var c = new TestdataGroupChainValue("C", Duration.ofMinutes(10), List.of());
        var a = new TestdataGroupChainValue("A", Duration.ofMinutes(20), List.of(c));
        var b = new TestdataGroupChainValue("B", Duration.ofMinutes(30), List.of());
        var d = new TestdataGroupChainValue("D", Duration.ofMinutes(5), List.of(c));
        var x = new TestdataGroupChainValue("X", Duration.ofMinutes(40), List.of());
        TestdataGroupChainValue.align(a, b);
        entity1.getValues().addAll(List.of(c, a));
        entity2.getValues().addAll(List.of(b, d));
        var solution = new TestdataGroupChainSolution(List.of(entity1, entity2), List.of(a, b, c, d, x));

        var graph = buildGraph(entity1, entity2, a, b, c, d, x);
        // A and B wait for each other's ready time, so their start time keeps a node apart from their ready time.
        // The handover time reads only the end time, so it joins the node of the start and end time.
        // The report time also reads the ready time, which is before the kept split, so it stays apart.
        var keptSplitLayout = List.of(List.of("readyTime"), List.of("startTime", "endTime", "handoverTime"),
                List.of("reportTime"));
        assertThat(variablesPerNode(graph, a)).containsExactlyInAnyOrderElementsOf(keptSplitLayout);
        assertThat(variablesPerNode(graph, b)).containsExactlyInAnyOrderElementsOf(keptSplitLayout);
        // D has a dependency, but no aligned values, so nothing keeps a split.
        assertThat(variablesPerNode(graph, d)).containsExactly(ALL_VARIABLES);

        var solutionMetaModel = PlanningSolutionMetaModel.of(TestdataGroupChainSolution.class,
                TestdataGroupChainEntity.class, TestdataGroupChainValue.class);
        var valuesMetaModel = solutionMetaModel.genuineEntity(TestdataGroupChainEntity.class)
                .listVariable("values", TestdataGroupChainValue.class);
        var moveTestContext = MoveTester.build(solutionMetaModel).using(solution);
        assertInitialTimes(a, b, d);

        // X before C delays C, then A; B starts with A, which delays B and then D.
        moveTestContext.executeTemporarily(Moves.assign(valuesMetaModel, x, entity1, 0), view -> {
            assertTimes(a, 50, 50, 70, 70, 70);
            assertTimes(b, 15, 50, 80, 80, 115);
            assertTimes(d, 80, 80, 85, 85, 85);
        });

        assertInitialTimes(a, b, d);
    }

    private static void assertInitialTimes(TestdataGroupChainValue a, TestdataGroupChainValue b,
            TestdataGroupChainValue d) {
        // A waits 5 minutes for B to be ready, which delays its report by 5 minutes.
        assertTimes(a, 10, 15, 35, 35, 40);
        assertTimes(b, 15, 15, 45, 45, 45);
        assertTimes(d, 45, 45, 50, 50, 50);
    }

    @SuppressWarnings("unchecked")
    private static AbstractVariableReferenceGraph<TestdataGroupChainSolution, ?> buildGraph(Object... entities) {
        var solutionDescriptor = TestdataGroupChainSolution.buildSolutionDescriptor();
        assertThat(GraphStructure.determineGraphStructure(solutionDescriptor, entities).structure())
                .isEqualTo(GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE);
        return (AbstractVariableReferenceGraph<TestdataGroupChainSolution, ?>) DefaultShadowVariableSessionFactory
                .buildGraph(new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                        ChangedVariableNotifier.empty(), entities));
    }

    private static List<List<String>> variablesPerNode(AbstractVariableReferenceGraph<?, ?> graph, Object entity) {
        return graph.nodeList.stream()
                .filter(node -> node.entity() == entity)
                .map(node -> node.variableReferences().stream()
                        .map(variable -> variable.id().name())
                        .toList())
                .toList();
    }

    private static void assertTimes(TestdataGroupChainValue value, int readyMinute, int startMinute, int endMinute,
            int handoverMinute, int reportMinute) {
        assertThat(value.getReadyTime()).as("ready time of %s", value.getId()).isEqualTo(START.plusMinutes(readyMinute));
        assertThat(value.getStartTime()).as("start time of %s", value.getId()).isEqualTo(START.plusMinutes(startMinute));
        assertThat(value.getEndTime()).as("end time of %s", value.getId()).isEqualTo(START.plusMinutes(endMinute));
        assertThat(value.getHandoverTime()).as("handover time of %s", value.getId())
                .isEqualTo(START.plusMinutes(handoverMinute));
        assertThat(value.getReportTime()).as("report time of %s", value.getId()).isEqualTo(START.plusMinutes(reportMinute));
    }
}
