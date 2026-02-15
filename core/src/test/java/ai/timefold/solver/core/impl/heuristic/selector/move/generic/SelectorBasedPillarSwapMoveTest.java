package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfCollection;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class SelectorBasedPillarSwapMoveTest {

    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var v5 = new TestdataValue("5");

        var a = new TestdataAllowsUnassignedEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("c", Arrays.asList(v4, v5), null);
        var z = new TestdataAllowsUnassignedEntityProvidingEntity("z", Arrays.asList(v1, v2, v3, v4, v5), null);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(Arrays.asList(a, b, c, z));

        var valueRangeManager =
                ValueRangeManager.of(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor(), solution);
        var scoreDirector = (VariableDescriptorAwareScoreDirector<TestdataAllowsUnassignedEntityProvidingSolution>) mock(
                VariableDescriptorAwareScoreDirector.class);
        doReturn(valueRangeManager).when(scoreDirector).getValueRangeManager();

        var variableDescriptorList =
                TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor().getGenuineVariableDescriptorList();

        var abMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(a), List.of(b));
        a.setValue(v1);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v3);
        assertThat(abMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v3);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v3);
        b.setValue(v3);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v4);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();

        var acMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(a), List.of(c));
        a.setValue(v1);
        c.setValue(v4);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        c.setValue(v5);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();

        var bcMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(b), List.of(c));
        b.setValue(v2);
        c.setValue(v4);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isFalse();
        b.setValue(v4);
        c.setValue(v5);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isTrue();
        b.setValue(v5);
        c.setValue(v4);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isTrue();
        b.setValue(v5);
        c.setValue(v5);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isFalse();

        var abzMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(a, b), List.of(z));
        a.setValue(v2);
        b.setValue(v2);
        z.setValue(v4);
        assertThat(abzMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        z.setValue(v1);
        assertThat(abzMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        z.setValue(v3);
        assertThat(abzMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v3);
        b.setValue(v3);
        z.setValue(v2);
        assertThat(abzMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v2);
        b.setValue(v2);
        z.setValue(v2);
        assertThat(abzMove.isMoveDoable(scoreDirector)).isFalse();
    }

    @Test
    void doMove() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var v5 = new TestdataValue("5");

        var a = new TestdataAllowsUnassignedEntityProvidingEntity("a", Arrays.asList(v1, v2, v3, v4), null);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("c", Arrays.asList(v4, v5), null);
        var z = new TestdataAllowsUnassignedEntityProvidingEntity("z", Arrays.asList(v1, v2, v3, v4, v5), null);

        var scoreDirectorFactory =
                new EasyScoreDirectorFactory<>(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor(),
                        solution -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        var variableDescriptorList =
                TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor().getGenuineVariableDescriptorList();

        var abMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(a), List.of(b));

        a.setValue(v1);
        b.setValue(v1);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v1);
        assertThat(b.getValue()).isEqualTo(v1);

        a.setValue(v2);
        b.setValue(v1);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v1);
        assertThat(b.getValue()).isEqualTo(v2);

        a.setValue(v3);
        b.setValue(v2);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v3);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v2);

        var abzMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(a, b), List.of(z));

        a.setValue(v3);
        b.setValue(v3);
        z.setValue(v2);
        abzMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(z.getValue()).isEqualTo(v3);
        abzMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(z.getValue()).isEqualTo(v2);

        a.setValue(v3);
        b.setValue(v3);
        z.setValue(v4);
        abzMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v4);
        assertThat(b.getValue()).isEqualTo(v4);
        assertThat(z.getValue()).isEqualTo(v3);
        abzMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(z.getValue()).isEqualTo(v4);

        var abczMove = new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(a), Arrays.asList(b, c, z));

        a.setValue(v2);
        b.setValue(v3);
        c.setValue(v3);
        z.setValue(v3);
        abczMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);
        assertThat(z.getValue()).isEqualTo(v2);
        abczMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v3);
        assertThat(z.getValue()).isEqualTo(v3);

        var abczMove2 = new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(a, b), Arrays.asList(c, z));

        a.setValue(v4);
        b.setValue(v4);
        c.setValue(v3);
        z.setValue(v3);
        abczMove2.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v4);
        assertThat(z.getValue()).isEqualTo(v4);
        abczMove2.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v4);
        assertThat(b.getValue()).isEqualTo(v4);
        assertThat(c.getValue()).isEqualTo(v3);
        assertThat(z.getValue()).isEqualTo(v3);
    }

    @Test
    void rebase() {
        var entityDescriptor = TestdataEntity.buildEntityDescriptor();
        var variableDescriptorList = entityDescriptor
                .getGenuineVariableDescriptorList();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", null);
        var e3 = new TestdataEntity("e3", v1);
        var e4 = new TestdataEntity("e4", v3);

        var destinationV1 = new TestdataValue("v1");
        var destinationV2 = new TestdataValue("v2");
        var destinationV3 = new TestdataValue("v3");
        var destinationE1 = new TestdataEntity("e1", destinationV1);
        var destinationE2 = new TestdataEntity("e2", null);
        var destinationE3 = new TestdataEntity("e3", destinationV1);
        var destinationE4 = new TestdataEntity("e4", destinationV3);

        InnerScoreDirector<TestdataSolution, ?> destinationScoreDirector = mockRebasingScoreDirector(
                entityDescriptor.getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { v3, destinationV3 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                        { e4, destinationE4 },
                });

        assertSameProperties(Arrays.asList(destinationE1, destinationE3), List.of(destinationE2),
                new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(e1, e3), List.of(e2))
                        .rebase(destinationScoreDirector.getMoveDirector()));
        assertSameProperties(List.of(destinationE4), Arrays.asList(destinationE1, destinationE3),
                new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(e4), Arrays.asList(e1, e3))
                        .rebase(destinationScoreDirector.getMoveDirector()));
    }

    public void assertSameProperties(List<Object> leftPillar, List<Object> rightPillar, SelectorBasedPillarSwapMove<?> move) {
        assertThat(move.getLeftPillar()).hasSameElementsAs(leftPillar);
        assertThat(move.getRightPillar()).hasSameElementsAs(rightPillar);
    }

    @Test
    void getters() {
        var primaryDescriptor = TestdataMultiVarEntity
                .buildVariableDescriptorForPrimaryValue();
        var secondaryDescriptor = TestdataMultiVarEntity
                .buildVariableDescriptorForSecondaryValue();
        var move = new SelectorBasedPillarSwapMove<>(Collections.singletonList(primaryDescriptor),
                Arrays.asList(new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b")),
                Arrays.asList(new TestdataMultiVarEntity("c"), new TestdataMultiVarEntity("d")));
        assertThat(move.getVariableNameList()).containsExactly("primaryValue");
        assertAllCodesOfCollection(move.getLeftPillar(), "a", "b");
        assertAllCodesOfCollection(move.getRightPillar(), "c", "d");

        move = new SelectorBasedPillarSwapMove<>(Arrays.asList(primaryDescriptor, secondaryDescriptor),
                Arrays.asList(new TestdataMultiVarEntity("e"), new TestdataMultiVarEntity("f")),
                Arrays.asList(new TestdataMultiVarEntity("g"), new TestdataMultiVarEntity("h")));
        assertThat(move.getVariableNameList()).containsExactly("primaryValue", "secondaryValue");
        assertAllCodesOfCollection(move.getLeftPillar(), "e", "f");
        assertAllCodesOfCollection(move.getRightPillar(), "g", "h");
    }

    @Test
    void toStringTest() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var a = new TestdataEntity("a", null);
        var b = new TestdataEntity("b", null);
        var c = new TestdataEntity("c", v1);
        var d = new TestdataEntity("d", v1);
        var e = new TestdataEntity("e", v1);
        var f = new TestdataEntity("f", v2);
        var g = new TestdataEntity("g", v2);
        var variableDescriptorList = TestdataEntity.buildEntityDescriptor()
                .getGenuineVariableDescriptorList();

        assertThat(new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(a, b), Arrays.asList(c, d, e)))
                .hasToString("[a, b] {null} <-> [c, d, e] {v1}");
        assertThat(new SelectorBasedPillarSwapMove<>(variableDescriptorList, List.of(b), List.of(c)))
                .hasToString("[b] {null} <-> [c] {v1}");
        assertThat(new SelectorBasedPillarSwapMove<>(variableDescriptorList, Arrays.asList(f, g), Arrays.asList(c, d, e)))
                .hasToString("[f, g] {v2} <-> [c, d, e] {v1}");
    }

}
