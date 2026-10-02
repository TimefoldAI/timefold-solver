package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class PillarSwapMoveTest {

    @Test
    void bothOldValuesAreReadBeforeEitherPillarIsMutated() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e1.setValue(v1);
        e2.setValue(v2);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var leftPillar = Sample.of(List.of(e1));
        var rightPillar = Sample.of(List.of(e2));
        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel), leftPillar, rightPillar);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(pillarSwapMove);

        // Correct: left ends up on v2, right ends up on v1.
        assertThat(e1.getValue()).isEqualTo(v2);
        assertThat(e2.getValue()).isEqualTo(v1);
        // A naive write-left-then-read-right implementation would leave both entities on v2.
        assertThat(e2.getValue()).isNotEqualTo(v2);
    }

    @Test
    void unequalSizedPillarsSwapEveryMember() {
        var solution = TestdataSolution.generateSolution(2, 8);
        var entityList = solution.getEntityList();
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);

        var leftEntities = entityList.subList(0, 3); // Size 3.
        var rightEntities = entityList.subList(3, 8); // Size 5.
        for (var entity : leftEntities) {
            entity.setValue(v1);
        }
        for (var entity : rightEntities) {
            entity.setValue(v2);
        }

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var leftPillar = Sample.of(leftEntities);
        var rightPillar = Sample.of(rightEntities);
        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel), leftPillar, rightPillar);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(pillarSwapMove);

        for (var entity : leftEntities) {
            assertThat(entity.getValue()).isEqualTo(v2);
        }
        for (var entity : rightEntities) {
            assertThat(entity.getValue()).isEqualTo(v1);
        }
    }

    @Test
    void multiVariateSwapsEveryDifferingVariable() {
        var solution = TestdataMultiVarSolution.generateSolution(4, 4, 2);
        var entityList = solution.getMultiVarEntityList();
        var valueList = solution.getValueList();
        var leftA = entityList.get(0);
        var leftB = entityList.get(1);
        var rightA = entityList.get(2);
        var rightB = entityList.get(3);

        leftA.setPrimaryValue(valueList.get(0));
        leftA.setSecondaryValue(valueList.get(1));
        leftB.setPrimaryValue(valueList.get(0));
        leftB.setSecondaryValue(valueList.get(1));
        rightA.setPrimaryValue(valueList.get(2));
        rightA.setSecondaryValue(valueList.get(3));
        rightB.setPrimaryValue(valueList.get(2));
        rightB.setSecondaryValue(valueList.get(3));

        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataMultiVarEntity.class);
        var variableMetaModelList = allVariablesExceptTertiary(entityMetaModel);

        var leftPillar = Sample.of(List.of(leftA, leftB));
        var rightPillar = Sample.of(List.of(rightA, rightB));
        var pillarSwapMove = Moves.pillarSwap(variableMetaModelList, leftPillar, rightPillar);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(pillarSwapMove);

        assertThat(leftA.getPrimaryValue()).isEqualTo(valueList.get(2));
        assertThat(leftA.getSecondaryValue()).isEqualTo(valueList.get(3));
        assertThat(leftB.getPrimaryValue()).isEqualTo(valueList.get(2));
        assertThat(leftB.getSecondaryValue()).isEqualTo(valueList.get(3));
        assertThat(rightA.getPrimaryValue()).isEqualTo(valueList.get(0));
        assertThat(rightA.getSecondaryValue()).isEqualTo(valueList.get(1));
        assertThat(rightB.getPrimaryValue()).isEqualTo(valueList.get(0));
        assertThat(rightB.getSecondaryValue()).isEqualTo(valueList.get(1));
    }

    @Test
    void outOfRangeCrossValueIsCallerResponsibility() {
        // PillarSwapMove no longer re-checks range at execution time;
        // that check is PillarSwapMoveProvider/SubPillarSwapMoveProvider's job.
        // A hand-built move over an invalid pair now writes the out-of-range value,
        // which the score director's value-range assertion catches.
        // The built-in providers never propose such a pair.
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class).basicVariable();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");

        // e1b's range does not include v2: this pair is invalid for a swap.
        var e1a = new TestdataEntityProvidingEntity("e1a", List.of(v1, v2));
        e1a.setValue(v1);
        var e1b = new TestdataEntityProvidingEntity("e1b", List.of(v1));
        e1b.setValue(v1);
        var e2 = new TestdataEntityProvidingEntity("e2", List.of(v1, v2));
        e2.setValue(v2);

        var solution = new TestdataEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1a, e1b, e2));

        var variableMetaModelList = singleVariableList(variableMetaModel);

        // The provider would never propose this move; it is constructed by hand to exercise caller misuse.
        var invalidMove = Moves.pillarSwap(variableMetaModelList, Sample.of(List.of(e1a, e1b)), Sample.of(List.of(e2)));

        var context = MoveTester.build(solutionMetaModel).using(solution);

        assertThatIllegalStateException()
                .isThrownBy(() -> context.execute(invalidMove))
                .withMessageContaining("outside of the related value range");
    }

    @Test
    void undoRestoresEveryMembersOriginalValue() {
        var solution = TestdataSolution.generateSolution(2, 4);
        var entityList = solution.getEntityList();
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var left1 = entityList.get(0);
        var left2 = entityList.get(1);
        var right1 = entityList.get(2);
        var right2 = entityList.get(3);
        left1.setValue(v1);
        left2.setValue(v1);
        right1.setValue(v2);
        right2.setValue(v2);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel),
                Sample.of(List.of(left1, left2)), Sample.of(List.of(right1, right2)));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(pillarSwapMove, view -> {
                    assertThat(left1.getValue()).isEqualTo(v2);
                    assertThat(left2.getValue()).isEqualTo(v2);
                    assertThat(right1.getValue()).isEqualTo(v1);
                    assertThat(right2.getValue()).isEqualTo(v1);
                });

        assertThat(left1.getValue()).isEqualTo(v1);
        assertThat(left2.getValue()).isEqualTo(v1);
        assertThat(right1.getValue()).isEqualTo(v2);
        assertThat(right2.getValue()).isEqualTo(v2);
    }

    @Test
    void getPlanningEntitiesReturnsMembersOfBothPillars() {
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel), Sample.of(List.of(e1, e2)),
                Sample.of(List.of(e3)));

        assertThat(pillarSwapMove.getPlanningEntities()).containsExactlyInAnyOrder(e1, e2, e3);
    }

    @Test
    void getPlanningValuesReturnsTheValuesThatChangedHands() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e1.setValue(v1);
        e2.setValue(v2);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel), Sample.of(List.of(e1)),
                Sample.of(List.of(e2)));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(pillarSwapMove);

        assertThat(pillarSwapMove.getPlanningValues()).containsExactlyInAnyOrder(v1, v2);
    }

    @Test
    void equalsAndHashCodeIgnoreMemberDrawOrderWithinEachPillar() {
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");
        var e4 = new TestdataEntity("e4");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var variableMetaModelList = singleVariableList(variableMetaModel);

        var move1 = Moves.pillarSwap(variableMetaModelList, Sample.of(List.of(e1, e2)), Sample.of(List.of(e3, e4)));
        var move2 = Moves.pillarSwap(variableMetaModelList, Sample.of(List.of(e2, e1)), Sample.of(List.of(e4, e3)));
        var move3 = Moves.pillarSwap(variableMetaModelList, Sample.of(List.of(e1, e2)), Sample.of(List.of(e3)));

        // Same members in a different draw order are equal.
        assertThat(move1).isEqualTo(move2);
        assertThat(move1.hashCode()).isEqualTo(move2.hashCode());

        // A different right pillar is not equal.
        assertThat(move1).isNotEqualTo(move3);

        assertThat(move1).isNotEqualTo(null);
        assertThat(move1).isNotEqualTo("not a move");
    }

    @Test
    void describeIncludesEntityAndVariableName() {
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var pillarSwapMove = Moves.pillarSwap(singleVariableList(variableMetaModel), Sample.of(List.of(e1)),
                Sample.of(List.of(e2)));

        assertThat(pillarSwapMove.describe()).isEqualTo("PillarSwapMove(TestdataEntity.value)");
    }

    @Test
    void rebaseReturnsMoveWhosePillarsHoldWorkingSolutionCopies() {
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var rebasedE1 = new TestdataEntity("e1");
        var rebasedE2 = new TestdataEntity("e2");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var originalMove =
                (PillarSwapMove<TestdataSolution, TestdataEntity>) Moves.pillarSwap(singleVariableList(variableMetaModel),
                        Sample.of(List.of(e1)), Sample.of(List.of(e2)));

        var rebasedMove = originalMove.rebase(new Lookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T lookUpWorkingObject(T object) {
                if (object == e1) {
                    return (T) rebasedE1;
                } else if (object == e2) {
                    return (T) rebasedE2;
                }
                return object;
            }
        });

        assertThat(rebasedMove.getPlanningEntities()).containsExactlyInAnyOrder(rebasedE1, rebasedE2);
    }

    @SuppressWarnings("unchecked")
    private static <Solution_, Entity_, Value_> List<PlanningVariableMetaModel<Solution_, Entity_, Object>>
            singleVariableList(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return List.of((PlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel);
    }

    @SuppressWarnings("unchecked")
    private static List<PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>>
            allVariablesExceptTertiary(
                    PlanningEntityMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity> entityMetaModel) {
        return entityMetaModel.variables().stream()
                .filter(v -> !v.name().contains("tertiary"))
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>) v)
                .toList();
    }

}
