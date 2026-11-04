package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.assertRandomListMovesMatchArbitraryGraph;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyValue;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVisit;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListEntity;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue;
import ai.timefold.solver.core.testdomain.shadow.simple_list_next.TestdataDeclarativeSimpleNextListEntity;
import ai.timefold.solver.core.testdomain.shadow.simple_list_next.TestdataDeclarativeSimpleNextListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list_next.TestdataDeclarativeSimpleNextListValue;

import org.junit.jupiter.api.Test;

/**
 * The graph structures other than {@link GraphStructure#ARBITRARY} are optimizations,
 * so they must compute the same shadow variables as the arbitrary graph.
 * Unlike {@link ai.timefold.solver.core.config.solver.EnvironmentMode#FULL_ASSERT},
 * which recomputes the shadow variables with the graph under test,
 * these tests compare that graph against the arbitrary graph.
 * <p>
 * Each test uses an existing test domain whose shadow variables select the structure under test:
 * <ul>
 * <li>{@link GraphStructure#SINGLE_DIRECTIONAL_PARENT}: {@code simple_list} (previous)
 * and {@code simple_list_next} (next)</li>
 * <li>{@link GraphStructure#ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE}: {@code dependency}
 * (dependencies between values, which can loop) and {@code concurrent} (values aligned in groups)</li>
 * <li>{@link GraphStructure#LIST_CHAIN}: {@code multi_entity_chain} (previous)
 * and {@code multi_entity_chain_next} (next)</li>
 * </ul>
 */
class GraphStructureDifferentialTest {

    private static final int ENTITY_COUNT = 3;
    private static final int VALUE_COUNT = 9;
    private static final int MOVE_COUNT = 1000;

    @Test
    void singleDirectionalPreviousParent() {
        var entityList = new ArrayList<TestdataDeclarativeSimpleListEntity>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            entityList.add(new TestdataDeclarativeSimpleListEntity("e" + i, i, 10 * i));
        }
        var valueList = new ArrayList<TestdataDeclarativeSimpleListValue>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            valueList.add(new TestdataDeclarativeSimpleListValue("v" + i, i, 1 + i % 3));
        }
        assignInitially(entityList, valueList, TestdataDeclarativeSimpleListEntity::getValues);
        var solutionDescriptor = TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor,
                new TestdataDeclarativeSimpleListSolution(entityList, valueList), GraphStructure.SINGLE_DIRECTIONAL_PARENT,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataDeclarativeSimpleListEntity.class)
                        .listVariable("values", TestdataDeclarativeSimpleListValue.class),
                TestdataDeclarativeSimpleListEntity::getValues, entityList, valueList, MOVE_COUNT);
    }

    @Test
    void singleDirectionalNextParent() {
        var entityList = new ArrayList<TestdataDeclarativeSimpleNextListEntity>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            entityList.add(new TestdataDeclarativeSimpleNextListEntity("e" + i, i, 100 + 10 * i));
        }
        var valueList = new ArrayList<TestdataDeclarativeSimpleNextListValue>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            valueList.add(new TestdataDeclarativeSimpleNextListValue("v" + i, 1 + i % 3));
        }
        assignInitially(entityList, valueList, TestdataDeclarativeSimpleNextListEntity::getValues);
        var solutionDescriptor = TestdataDeclarativeSimpleNextListSolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor,
                new TestdataDeclarativeSimpleNextListSolution(entityList, valueList),
                GraphStructure.SINGLE_DIRECTIONAL_PARENT,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataDeclarativeSimpleNextListEntity.class)
                        .listVariable("values", TestdataDeclarativeSimpleNextListValue.class),
                TestdataDeclarativeSimpleNextListEntity::getValues, entityList, valueList, MOVE_COUNT);
    }

    @Test
    void listChainPreviousParent() {
        var entityList = new ArrayList<TestdataMultiEntityChainVehicle>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            var entity = new TestdataMultiEntityChainVehicle("e" + i, 5 * i);
            if (i > 0) { // Each vehicle starts where the previous one ends.
                entity.setPreviousVehicles(List.of(entityList.get(i - 1)));
            }
            entityList.add(entity);
        }
        var valueList = new ArrayList<TestdataMultiEntityChainVisit>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            valueList.add(new TestdataMultiEntityChainVisit("v" + i, 1 + i % 3, i % 4 != 3));
        }
        assignInitially(entityList, valueList, TestdataMultiEntityChainVehicle::getVisits);
        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(entityList);
        solution.setVisits(valueList);
        var solutionDescriptor = TestdataMultiEntityChainSolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor, solution, GraphStructure.LIST_CHAIN,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataMultiEntityChainVehicle.class)
                        .listVariable("visits", TestdataMultiEntityChainVisit.class),
                TestdataMultiEntityChainVehicle::getVisits, entityList, valueList, MOVE_COUNT);
    }

    @Test
    void listChainNextParent() {
        var entityList = new ArrayList<TestdataMultiEntityChainNextVehicle>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            entityList.add(new TestdataMultiEntityChainNextVehicle("e" + i, 100));
        }
        for (var i = 0; i < ENTITY_COUNT - 1; i++) { // Each vehicle ends where the next one starts.
            entityList.get(i).getNextVehicles().add(entityList.get(i + 1));
        }
        var valueList = new ArrayList<TestdataMultiEntityChainNextVisit>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            valueList.add(new TestdataMultiEntityChainNextVisit("v" + i, 1 + i % 3));
        }
        assignInitially(entityList, valueList, TestdataMultiEntityChainNextVehicle::getVisits);
        var solution = new TestdataMultiEntityChainNextSolution();
        solution.setVehicles(entityList);
        solution.setVisits(valueList);
        var solutionDescriptor = TestdataMultiEntityChainNextSolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor, solution, GraphStructure.LIST_CHAIN,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataMultiEntityChainNextVehicle.class)
                        .listVariable("visits", TestdataMultiEntityChainNextVisit.class),
                TestdataMultiEntityChainNextVehicle::getVisits, entityList, valueList, MOVE_COUNT);
    }

    @Test
    void arbitrarySingleEntity() {
        var entityList = new ArrayList<TestdataDependencyEntity>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            entityList.add(new TestdataDependencyEntity(LocalDateTime.of(2025, 1, 1, 9 + i, 0)));
        }
        var valueList = new ArrayList<TestdataDependencyValue>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            // Every third value depends on the two values before it; the list order can make them loop.
            var dependencyList = i % 3 == 2 ? List.of(valueList.get(i - 2), valueList.get(i - 1)) : null;
            valueList.add(new TestdataDependencyValue("v" + i, Duration.ofMinutes(10L + i), dependencyList));
        }
        assignInitially(entityList, valueList, TestdataDependencyEntity::getValues);
        var solutionDescriptor = TestdataDependencySolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor,
                new TestdataDependencySolution(entityList, valueList),
                GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataDependencyEntity.class)
                        .listVariable("values", TestdataDependencyValue.class),
                TestdataDependencyEntity::getValues, entityList, valueList, MOVE_COUNT);
    }

    @Test
    void arbitrarySingleEntityWithAlignedValues() {
        var entityList = new ArrayList<TestdataConcurrentEntity>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            entityList.add(new TestdataConcurrentEntity("e" + i));
        }
        var valueList = new ArrayList<TestdataConcurrentValue>();
        for (var i = 0; i < VALUE_COUNT; i++) {
            var value = new TestdataConcurrentValue("v" + i);
            valueList.add(value);
            entityList.get(i % ENTITY_COUNT).getValues().add(value);
        }
        // Groups of 2 and 3 values; the other values belong to no group.
        for (var group : List.of(valueList.subList(0, 2), valueList.subList(2, 5))) {
            group.forEach(value -> value.setConcurrentValueGroup(group));
        }
        var solution = new TestdataConcurrentSolution();
        solution.setEntities(entityList);
        solution.setValues(valueList);
        var solutionDescriptor = TestdataConcurrentSolution.buildSolutionDescriptor();
        assertRandomListMovesMatchArbitraryGraph(solutionDescriptor, solution,
                GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE,
                solutionDescriptor.getMetaModel().genuineEntity(TestdataConcurrentEntity.class)
                        .listVariable("values", TestdataConcurrentValue.class),
                TestdataConcurrentEntity::getValues, entityList, valueList, MOVE_COUNT);
    }

    /**
     * Assigns the values to the entities in turn, leaving the last third unassigned.
     */
    private static <Entity_, Value_> void assignInitially(List<Entity_> entityList, List<Value_> valueList,
            Function<Entity_, List<Value_>> listFunction) {
        for (var i = 0; i < 2 * valueList.size() / 3; i++) {
            listFunction.apply(entityList.get(i % entityList.size())).add(valueList.get(i));
        }
    }
}
