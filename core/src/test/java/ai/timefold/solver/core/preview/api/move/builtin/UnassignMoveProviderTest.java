package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class UnassignMoveProviderTest {

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataPinnedAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);

        // firstEntity is pinned → no unassign moves. Only secondEntity can be unassigned.
        var moveList = NeighborhoodTester.build(new UnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues()).containsExactly((Object) null);
        });
    }

    @Test
    void unassignedEntitySkipped() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable("value", TestdataValue.class);

        // generateSolution(2, 2): entity0=null, entity1=value1
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(1); // non-null value

        var moveList = NeighborhoodTester.build(new UnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var move = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(move.getPlanningEntities()).containsExactly(entity1);
            softly.assertThat(move.getPlanningValues()).containsExactly((Object) null);
        });
    }

    @Test
    void allEntitiesAssigned() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        solution.getEntityList().getFirst().setValue(solution.getValueList().getFirst());

        var moveList = NeighborhoodTester.build(new UnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();
        assertThat(moveList).hasSize(2);
    }

    @Test
    void noAssignedEntities() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(e -> e.setValue(null));

        var moveList = NeighborhoodTester.build(new UnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();
        assertThat(moveList).isEmpty();
    }

    @Test
    void failsOnNonNullableVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnassignMoveProvider<>(variableMetaModel));
    }

}
