package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Direct test of {@link DefaultMoveStreamFactory#forEachDestination} and
 * {@link DefaultMoveStreamFactory#forEachDestinationIncludingUnassigned}: the exact set of positions they produce, not just
 * membership as the builtin/example move provider tests exercise indirectly.
 * <p>
 * These two methods no longer build their destination set via a join between entities and values (see the class's
 * javadoc history) - the join used to duplicate a value's position once per unpinned entity whose value range
 * accepted the value, relying on {@code .distinct()} to collapse the duplicates back to one row.
 * The rewrite instead concatenates two join-free streams: one row per unpinned assigned value (that value's own position) and
 * one row per unpinned entity (the end-of-list slot).
 * This pins down that the two are equivalent, including for entities with disjoint, entity-provided value ranges - the one case
 * where the dropped value-range check could theoretically have mattered.
 */
@NullMarked
class DefaultMoveStreamFactoryTest {

    private static <Solution_> DefaultNeighborhoodSession<Solution_> createSession(
            DefaultMoveStreamFactory<Solution_> factory,
            SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT)
                .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = factory.createSession(new SessionContext<>(scoreDirector));
        solutionDescriptor.visitAll(solution, session::insert);
        session.settle();
        return session;
    }

    @Test
    void forEachDestination_valueAndEndOfListPositions() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataListEntity.class)
                .listVariable();
        var factory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var dataset = ((AbstractUniEnumeratingStream<TestdataListSolution, PositionInList>) factory
                .forEachDestination(variableMetaModel)).asCachedDataset();

        // Round-robin over 2 entities: e0 = [v0, v2], e1 = [v1, v3].
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var session = createSession(factory, solutionDescriptor, solution);
        var instance = session.getLeftDatasetInstance(dataset);

        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);

        assertThat(instance.iterator()).toIterable()
                .map(UniTuple::getA)
                .containsExactlyInAnyOrder(
                        // Insert-before an unpinned assigned value: one per value, at that value's own position.
                        ElementPosition.of(e0, 0), ElementPosition.of(e0, 1),
                        ElementPosition.of(e1, 0), ElementPosition.of(e1, 1),
                        // End-of-list slot: one per unpinned entity.
                        ElementPosition.of(e0, 2), ElementPosition.of(e1, 2));
    }

    @Test
    void forEachDestination_entityProvidedDisjointValueRanges() {
        // The one theoretical edge case the join-free rewrite leans on: entities whose value ranges don't overlap.
        // The old join's valueInRangeFilter never gated which positions appeared (a value's own entity always
        // satisfies its own range), so this must produce the exact same set as the plain case above.
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataListEntityProvidingEntity.class)
                .listVariable();
        var factory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var dataset = ((AbstractUniEnumeratingStream<TestdataListEntityProvidingSolution, PositionInList>) factory
                .forEachDestination(variableMetaModel)).asCachedDataset();

        // Disjoint value ranges: e0's range is only [v0], e1's range is only [v1].
        var solution = TestdataListEntityProvidingSolution.generateSolution(2, 2, false);
        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        e0.setValueList(new ArrayList<>(e0.getValueRange()));
        e1.setValueList(new ArrayList<>(e1.getValueRange()));
        SolutionManager.updateShadowVariables(solution);

        var session = createSession(factory, solutionDescriptor, solution);
        var instance = session.getLeftDatasetInstance(dataset);

        assertThat(instance.iterator()).toIterable()
                .map(UniTuple::getA)
                .containsExactlyInAnyOrder(
                        ElementPosition.of(e0, 0), ElementPosition.of(e1, 0),
                        ElementPosition.of(e0, 1), ElementPosition.of(e1, 1));
    }

    @Test
    void forEachDestinationIncludingUnassigned_addsExactlyOneUnassignedRow() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();
        var factory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var dataset =
                ((AbstractUniEnumeratingStream<TestdataAllowsUnassignedValuesListSolution, ElementPosition>) factory
                        .forEachDestinationIncludingUnassigned(variableMetaModel)).asCachedDataset();

        var value0 = new TestdataAllowsUnassignedValuesListValue("v0");
        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var entity0 = new TestdataAllowsUnassignedValuesListEntity("e0", value0, value1);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity0));
        solution.setValueList(List.of(value0, value1));
        SolutionManager.updateShadowVariables(solution);

        var session = createSession(factory, solutionDescriptor, solution);
        var instance = session.getLeftDatasetInstance(dataset);

        assertThat(instance.iterator()).toIterable()
                .map(UniTuple::getA)
                .containsExactlyInAnyOrder(
                        ElementPosition.of(entity0, 0), ElementPosition.of(entity0, 1), ElementPosition.of(entity0, 2),
                        ElementPosition.unassigned());
    }

}
