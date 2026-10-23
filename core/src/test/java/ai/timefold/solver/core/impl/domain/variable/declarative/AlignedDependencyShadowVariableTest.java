package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.aligned_dependency.TestdataAlignedDependencyConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.aligned_dependency.TestdataAlignedDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.aligned_dependency.TestdataAlignedDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.aligned_dependency.TestdataAlignedDependencyValue;

import org.junit.jupiter.api.Test;

/**
 * Covers an entity whose first declarative shadow variable has a group source ("dependencies[]")
 * and whose later declarative shadow variable has another group source ("alignedValues[]").
 */
class AlignedDependencyShadowVariableTest {

    private static final OffsetDateTime START = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final List<String> ALL_VARIABLES = List.of("readyTime", "startTime", "endTime");
    private static final List<String> READY_TIME = List.of("readyTime");
    private static final List<String> START_AND_END_TIME = List.of("startTime", "endTime");

    @Test
    void valueWithoutAlignedValuesKeepsAllItsVariablesInOneNode() {
        var a = new TestdataAlignedDependencyValue("A", Duration.ofMinutes(30));
        var b = new TestdataAlignedDependencyValue("B", Duration.ofMinutes(10), List.of(a));
        var c = new TestdataAlignedDependencyValue("C", Duration.ofMinutes(50));
        var d = new TestdataAlignedDependencyValue("D", Duration.ofMinutes(20));
        TestdataAlignedDependencyValue.align(c, d);

        var graph = buildGraph(a, b, c, d);

        // A uses no group at all.
        assertThat(variablesPerNode(graph, a)).containsExactly(ALL_VARIABLES);
        // B has a dependency, but no aligned values,
        // so its start time has nothing to wait for outside B.
        assertThat(variablesPerNode(graph, b)).containsExactly(ALL_VARIABLES);
    }

    @Test
    void alignedValueStartsANewNodeAtItsStartTime() {
        var a = new TestdataAlignedDependencyValue("A", Duration.ofMinutes(30));
        var c = new TestdataAlignedDependencyValue("C", Duration.ofMinutes(50));
        var d = new TestdataAlignedDependencyValue("D", Duration.ofMinutes(20));
        var e = new TestdataAlignedDependencyValue("E", Duration.ofMinutes(40), List.of(a));
        var f = new TestdataAlignedDependencyValue("F", Duration.ofMinutes(10));
        TestdataAlignedDependencyValue.align(c, d);
        TestdataAlignedDependencyValue.align(e, f);

        var graph = buildGraph(a, c, d, e, f);

        // C and D must wait for each other's ready time before they start,
        // which a single node per value would turn into a loop.
        assertThat(variablesPerNode(graph, c)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME);
        assertThat(variablesPerNode(graph, d)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME);
        // E has both a dependency and an aligned value.
        assertThat(variablesPerNode(graph, e)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME);
        assertThat(variablesPerNode(graph, f)).containsExactlyInAnyOrder(READY_TIME, START_AND_END_TIME);
    }

    @Test
    void valueThatDependsOnItselfFailsFast() {
        var a = new TestdataAlignedDependencyValue("A", Duration.ofMinutes(30));
        a.setDependencies(List.of(a));

        // The end time of A leads back to its ready time,
        // so the ready time must not share a node with the end time, which would hide the loop.
        assertThatCode(() -> buildGraph(a))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("There are fixed dependency loops in the graph");
    }

    @Test
    void moveUpdatesAValueWithOneNodeAndTheAlignedValues() {
        var entity1 = new TestdataAlignedDependencyEntity("E1", START);
        var entity2 = new TestdataAlignedDependencyEntity("E2", START);
        var a = new TestdataAlignedDependencyValue("A", Duration.ofMinutes(30));
        var b = new TestdataAlignedDependencyValue("B", Duration.ofMinutes(10), List.of(a));
        var c = new TestdataAlignedDependencyValue("C", Duration.ofMinutes(50));
        var d = new TestdataAlignedDependencyValue("D", Duration.ofMinutes(20));
        var x = new TestdataAlignedDependencyValue("X", Duration.ofMinutes(40));
        TestdataAlignedDependencyValue.align(c, d);
        entity1.getValues().addAll(List.of(a, c));
        entity2.getValues().addAll(List.of(d, b));
        var solution = new TestdataAlignedDependencySolution(List.of(entity1, entity2), List.of(a, b, c, d, x));

        var solutionMetaModel = PlanningSolutionMetaModel.of(TestdataAlignedDependencySolution.class,
                TestdataAlignedDependencyEntity.class, TestdataAlignedDependencyValue.class);
        var valuesMetaModel = solutionMetaModel.genuineEntity(TestdataAlignedDependencyEntity.class)
                .listVariable("values", TestdataAlignedDependencyValue.class);
        var moveTestContext = MoveTester.build(solutionMetaModel).using(solution);

        // C is ready when A ends, D is ready at once; both start when the later one is ready.
        // B waits for D (before it) and A (its dependency).
        assertTimes(a, 0, 0, 30);
        assertTimes(c, 30, 30, 80);
        assertTimes(d, 0, 30, 50);
        assertTimes(b, 50, 50, 60);

        // X before A delays A, which delays C, which delays D, which delays B.
        moveTestContext.executeTemporarily(Moves.assign(valuesMetaModel, x, entity1, 0), view -> {
            assertTimes(x, 0, 0, 40);
            assertTimes(a, 40, 40, 70);
            assertTimes(c, 70, 70, 120);
            assertTimes(d, 0, 70, 90);
            assertTimes(b, 90, 90, 100);
        });

        assertTimes(a, 0, 0, 30);
        assertTimes(c, 30, 30, 80);
        assertTimes(d, 0, 30, 50);
        assertTimes(b, 50, 50, 60);
        assertThat(x.getStartTime()).isNull();
    }

    @Test
    void solve() {
        var a = new TestdataAlignedDependencyValue("A", Duration.ofMinutes(30));
        var b = new TestdataAlignedDependencyValue("B", Duration.ofMinutes(10));
        var c = new TestdataAlignedDependencyValue("C", Duration.ofMinutes(50), List.of(a));
        var d = new TestdataAlignedDependencyValue("D", Duration.ofMinutes(120), List.of(b));
        var e = new TestdataAlignedDependencyValue("E", Duration.ofMinutes(40), List.of(a, b));
        var f = new TestdataAlignedDependencyValue("F", Duration.ofMinutes(20));
        var g = new TestdataAlignedDependencyValue("G", Duration.ofMinutes(60), List.of(c));
        var h = new TestdataAlignedDependencyValue("H", Duration.ofMinutes(30), List.of(d));
        var i = new TestdataAlignedDependencyValue("I", Duration.ofMinutes(10), List.of(e));
        TestdataAlignedDependencyValue.align(c, d);
        TestdataAlignedDependencyValue.align(e, f);
        TestdataAlignedDependencyValue.align(g, h, i);
        var values = List.of(a, b, c, d, e, f, g, h, i);
        var solution = new TestdataAlignedDependencySolution(
                List.of(new TestdataAlignedDependencyEntity("E1", START),
                        new TestdataAlignedDependencyEntity("E2", START.plusMinutes(15)),
                        new TestdataAlignedDependencyEntity("E3", START.plusMinutes(30)),
                        new TestdataAlignedDependencyEntity("E4", START.plusMinutes(45))),
                values);

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAlignedDependencySolution.class)
                .withEntityClasses(TestdataAlignedDependencyEntity.class, TestdataAlignedDependencyValue.class)
                .withConstraintProviderClass(TestdataAlignedDependencyConstraintProvider.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));
        var bestSolution = SolverFactory.<TestdataAlignedDependencySolution> create(solverConfig)
                .buildSolver()
                .solve(solution);

        for (var value : bestSolution.getValues()) {
            assertThat(value.getStartTime()).isNotNull();
            for (var dependency : value.getDependencies()) {
                assertThat(value.getStartTime()).isAfterOrEqualTo(dependency.getEndTime());
            }
            for (var alignedValue : value.getAlignedValues()) {
                assertThat(value.getStartTime()).isEqualTo(alignedValue.getStartTime());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static AbstractVariableReferenceGraph<TestdataAlignedDependencySolution, ?> buildGraph(Object... entities) {
        var solutionDescriptor = TestdataAlignedDependencySolution.buildSolutionDescriptor();
        assertThat(GraphStructure.determineGraphStructure(solutionDescriptor, entities).structure())
                .isEqualTo(GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE);
        return (AbstractVariableReferenceGraph<TestdataAlignedDependencySolution, ?>) DefaultShadowVariableSessionFactory
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

    private static void assertTimes(TestdataAlignedDependencyValue value, int readyMinute, int startMinute,
            int endMinute) {
        assertThat(value.getReadyTime()).as("ready time of %s", value.getId()).isEqualTo(START.plusMinutes(readyMinute));
        assertThat(value.getStartTime()).as("start time of %s", value.getId()).isEqualTo(START.plusMinutes(startMinute));
        assertThat(value.getEndTime()).as("end time of %s", value.getId()).isEqualTo(START.plusMinutes(endMinute));
    }
}
