package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfCollection;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
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

class SelectorBasedPillarChangeMoveTest {

    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var v5 = new TestdataValue("5");

        var a = new TestdataAllowsUnassignedEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(Arrays.asList(a, b));

        var valueRangeManager =
                ValueRangeManager.of(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor(), solution);
        var scoreDirector = (VariableDescriptorAwareScoreDirector<TestdataAllowsUnassignedEntityProvidingSolution>) mock(
                VariableDescriptorAwareScoreDirector.class);
        doReturn(valueRangeManager).when(scoreDirector).getValueRangeManager();

        var variableDescriptor = TestdataAllowsUnassignedEntityProvidingEntity.buildVariableDescriptorForValue();

        a.setValue(v2);
        b.setValue(v2);
        var abMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v1);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        abMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        abMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v3);
        assertThat(abMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v2);
        b.setValue(v2);
        abMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v4);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
    }

    @Test
    void doMove() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var v5 = new TestdataValue("5");

        var a = new TestdataAllowsUnassignedEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("c", Arrays.asList(v3, v4, v5), null);

        var scoreDirectorFactory =
                new EasyScoreDirectorFactory<>(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor(),
                        solution -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT);
        InnerScoreDirector<TestdataAllowsUnassignedEntityProvidingSolution, ?> scoreDirector =
                scoreDirectorFactory.buildScoreDirector();
        var variableDescriptor = TestdataAllowsUnassignedEntityProvidingEntity.buildVariableDescriptorForValue();

        var abMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v2);

        a.setValue(v3);
        b.setValue(v3);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v2);

        a.setValue(v2);
        b.setValue(v2);
        abMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v2);

        var abcMove = new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b, c),
                variableDescriptor, v2);

        a.setValue(v2);
        b.setValue(v2);
        c.setValue(v2);
        abcMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);

        a.setValue(v3);
        b.setValue(v3);
        c.setValue(v3);
        abcMove.execute(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);
    }

    @Test
    void rebase() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

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
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { v3, destinationV3 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                        { e4, destinationE4 },
                });

        assertSameProperties(Arrays.asList(destinationE1, destinationE3), null,
                new SelectorBasedPillarChangeMove<>(Arrays.asList(e1, e3), variableDescriptor, null)
                        .rebase(destinationScoreDirector.getMoveDirector()));
        assertSameProperties(Arrays.asList(destinationE1, destinationE3), destinationV3,
                new SelectorBasedPillarChangeMove<>(Arrays.asList(e1, e3), variableDescriptor, v3)
                        .rebase(destinationScoreDirector.getMoveDirector()));
        assertSameProperties(Arrays.asList(destinationE2), destinationV1,
                new SelectorBasedPillarChangeMove<>(Arrays.asList(e2), variableDescriptor, v1)
                        .rebase(destinationScoreDirector.getMoveDirector()));
        assertSameProperties(Arrays.asList(destinationE1), destinationV2,
                new SelectorBasedPillarChangeMove<>(Arrays.asList(e1), variableDescriptor, v2)
                        .rebase(destinationScoreDirector.getMoveDirector()));
    }

    public void assertSameProperties(List<Object> pillar, Object toPlanningVariable, SelectorBasedPillarChangeMove<?> move) {
        assertThat(move.getPillar()).hasSameElementsAs(pillar);
        assertThat(move.getToPlanningValue()).isSameAs(toPlanningVariable);
    }

    @Test
    void getters() {
        var move = new SelectorBasedPillarChangeMove<>(
                Arrays.asList(new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b")),
                TestdataMultiVarEntity.buildVariableDescriptorForPrimaryValue(), null);
        assertAllCodesOfCollection(move.getPillar(), "a", "b");
        assertThat(move.getVariableName()).isEqualTo("primaryValue");
        assertCode(null, move.getToPlanningValue());

        move = new SelectorBasedPillarChangeMove<>(
                Arrays.asList(new TestdataMultiVarEntity("c"), new TestdataMultiVarEntity("d")),
                TestdataMultiVarEntity.buildVariableDescriptorForSecondaryValue(), new TestdataValue("1"));
        assertAllCodesOfCollection(move.getPillar(), "c", "d");
        assertThat(move.getVariableName()).isEqualTo("secondaryValue");
        assertCode("1", move.getToPlanningValue());
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
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

        assertThat(new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v1))
                .hasToString("[a, b] {null -> v1}");
        assertThat(new SelectorBasedPillarChangeMove<>(Arrays.asList(a, b), variableDescriptor, v2))
                .hasToString("[a, b] {null -> v2}");
        assertThat(new SelectorBasedPillarChangeMove<>(Arrays.asList(c, d, e), variableDescriptor, null))
                .hasToString("[c, d, e] {v1 -> null}");
        assertThat(new SelectorBasedPillarChangeMove<>(Arrays.asList(c, d, e), variableDescriptor, v2))
                .hasToString("[c, d, e] {v1 -> v2}");
        assertThat(new SelectorBasedPillarChangeMove<>(Arrays.asList(d), variableDescriptor, v2)).hasToString("[d] {v1 -> v2}");
    }

}
