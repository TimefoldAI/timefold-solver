package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.group_split.TestdataGroupSplitEntity;
import ai.timefold.solver.core.testdomain.shadow.group_split.TestdataGroupSplitSolution;
import ai.timefold.solver.core.testdomain.shadow.group_split.TestdataGroupSplitValue;

import org.junit.jupiter.api.Test;

/**
 * Covers variables whose group sources are empty, but which must keep their own graph node for another reason.
 */
class GroupSplitShadowVariableTest {

    private static final LocalDateTime START = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    private static final List<String> READY_TIME = List.of("readyTime");
    private static final List<String> START_AND_END_TIME = List.of("startTime", "endTime");
    private static final List<String> HANDOVER_TIME = List.of("handoverTime");
    private static final PlanningSolutionMetaModel<TestdataGroupSplitSolution> SOLUTION_META_MODEL =
            PlanningSolutionMetaModel.of(TestdataGroupSplitSolution.class, TestdataGroupSplitEntity.class,
                    TestdataGroupSplitValue.class);

    @Test
    void sourceOnAnotherEntityKeepsTheVariableInItsOwnNode() {
        var entity1 = new TestdataGroupSplitEntity("E1", START);
        var entity2 = new TestdataGroupSplitEntity("E2", START.plusMinutes(15));
        var c = new TestdataGroupSplitValue("C", Duration.ofMinutes(10), List.of());
        var a = new TestdataGroupSplitValue("A", Duration.ofMinutes(20), List.of(c));
        var b = new TestdataGroupSplitValue("B", Duration.ofMinutes(30), List.of(c));
        // A and B wait for each other's ready time before they start,
        // which one node for the ready and the start time of each value would turn into a loop.
        a.setPeer(b);
        b.setPeer(a);
        entity1.getValues().addAll(List.of(c, a));
        entity2.getValues().add(b);
        var solution = new TestdataGroupSplitSolution(List.of(entity1, entity2), List.of(a, b, c));

        var graph = buildGraph(entity1, entity2, a, b, c);
        assertThat(variablesPerNode(graph, a)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME, HANDOVER_TIME);

        MoveTester.build(SOLUTION_META_MODEL).using(solution);
        assertThat(a.getReadyTime()).isEqualTo(START.plusMinutes(10));
        assertThat(a.getStartTime()).isEqualTo(START.plusMinutes(15));
        assertThat(b.getStartTime()).isEqualTo(START.plusMinutes(15));
    }

    @Test
    void groupPathThroughAPlanningVariableKeepsTheVariableInItsOwnNode() {
        var entity1 = new TestdataGroupSplitEntity("E1", START);
        var entity2 = new TestdataGroupSplitEntity("E2", START);
        var c = new TestdataGroupSplitValue("C", Duration.ofMinutes(10), List.of());
        var x = new TestdataGroupSplitValue("X", Duration.ofMinutes(20), List.of(c));
        var y0 = new TestdataGroupSplitValue("Y0", Duration.ofMinutes(30), List.of(x));
        var y = new TestdataGroupSplitValue("Y", Duration.ofMinutes(5), List.of());
        // Y is unassigned, so the value before it is unknown, but its group is not empty.
        x.setFollowers(List.of(y));
        entity1.getValues().add(y0);
        entity2.getValues().addAll(List.of(c, x));
        var solution = new TestdataGroupSplitSolution(List.of(entity1, entity2), List.of(c, x, y0, y));

        var graph = buildGraph(entity1, entity2, c, x, y0, y);
        assertThat(variablesPerNode(graph, x)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME, HANDOVER_TIME);

        var valuesMetaModel = SOLUTION_META_MODEL.genuineEntity(TestdataGroupSplitEntity.class)
                .listVariable("values", TestdataGroupSplitValue.class);
        var moveTestContext = MoveTester.build(SOLUTION_META_MODEL).using(solution);
        assertThat(x.getHandoverTime()).isEqualTo(START.plusMinutes(30));
        // Y after Y0 makes X hand over when Y0 ends, while Y0 waits for X to end.
        // One node for all variables of X would turn that into a loop.
        moveTestContext.executeTemporarily(Moves.assign(valuesMetaModel, y, entity1, 1),
                view -> assertThat(x.getHandoverTime()).isEqualTo(START.plusMinutes(60)));
        assertThat(x.getHandoverTime()).isEqualTo(START.plusMinutes(30));
    }

    @SuppressWarnings("unchecked")
    private static AbstractVariableReferenceGraph<TestdataGroupSplitSolution, ?> buildGraph(Object... entities) {
        var solutionDescriptor = TestdataGroupSplitSolution.buildSolutionDescriptor();
        assertThat(GraphStructure.determineGraphStructure(solutionDescriptor, entities).structure())
                .isEqualTo(GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE);
        return (AbstractVariableReferenceGraph<TestdataGroupSplitSolution, ?>) DefaultShadowVariableSessionFactory
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
}
