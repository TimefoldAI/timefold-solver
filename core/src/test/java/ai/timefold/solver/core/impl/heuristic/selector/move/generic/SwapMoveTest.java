package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class SwapMoveTest {

    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var v5 = new TestdataValue("5");

        var a = new TestdataEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        var b = new TestdataEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        var c = new TestdataEntityProvidingEntity("c", Arrays.asList(v4, v5), null);

        ScoreDirector<TestdataEntityProvidingSolution> scoreDirector = mock(ScoreDirector.class);
        var entityDescriptor = TestdataEntityProvidingEntity.buildEntityDescriptor();

        var abMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), a, b);
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

        var acMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), a, c);
        a.setValue(v1);
        c.setValue(v4);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        c.setValue(v5);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();

        var bcMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), b, c);
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
    }

    @Test
    void doMove() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");

        var a = new TestdataEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        var b = new TestdataEntityProvidingEntity("b", Arrays.asList(v1, v2, v3, v4), null);
        var c = new TestdataEntityProvidingEntity("c", Arrays.asList(v2, v3, v4), null);

        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(TestdataEntityProvidingSolution.buildSolutionDescriptor(),
                solution -> SimpleScore.ZERO);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        var entityDescriptor = TestdataEntityProvidingEntity.buildEntityDescriptor();

        var abMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), a, b);

        a.setValue(v1);
        b.setValue(v1);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v1);
        assertThat(b.getValue()).isEqualTo(v1);

        a.setValue(v1);
        b.setValue(v2);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v1);

        a.setValue(v2);
        b.setValue(v3);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v2);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v3);

        var acMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), a, c);

        a.setValue(v2);
        c.setValue(v2);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);

        a.setValue(v3);
        c.setValue(v2);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v3);

        a.setValue(v3);
        c.setValue(v4);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v4);
        assertThat(c.getValue()).isEqualTo(v3);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v4);

        var bcMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(), b, c);

        b.setValue(v2);
        c.setValue(v2);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);

        b.setValue(v2);
        c.setValue(v3);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v2);

        b.setValue(v2);
        c.setValue(v3);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v2);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v3);
    }

    @Test
    void rebase() {
        var entityDescriptor = TestdataEntity.buildEntityDescriptor();
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", null);
        var e3 = new TestdataEntity("e3", v1);

        var destinationV1 = new TestdataValue("v1");
        var destinationV2 = new TestdataValue("v2");
        var destinationE1 = new TestdataEntity("e1", destinationV1);
        var destinationE2 = new TestdataEntity("e2", null);
        var destinationE3 = new TestdataEntity("e3", destinationV1);

        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                entityDescriptor.getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        assertSameProperties(destinationE1, destinationE2,
                new SwapMove<>(variableDescriptorList, e1, e2).rebase(destinationScoreDirector));
        assertSameProperties(destinationE1, destinationE3,
                new SwapMove<>(variableDescriptorList, e1, e3).rebase(destinationScoreDirector));
        assertSameProperties(destinationE2, destinationE3,
                new SwapMove<>(variableDescriptorList, e2, e3).rebase(destinationScoreDirector));
    }

    public void assertSameProperties(Object leftEntity, Object rightEntity, SwapMove<?> move) {
        assertThat(move.getLeftEntity()).isSameAs(leftEntity);
        assertThat(move.getRightEntity()).isSameAs(rightEntity);
    }

    @Test
    void getters() {
        var primaryDescriptor = TestdataMultiVarEntity.buildVariableDescriptorForPrimaryValue();
        var secondaryDescriptor = TestdataMultiVarEntity.buildVariableDescriptorForSecondaryValue();
        var move = new SwapMove<>(Collections.singletonList(primaryDescriptor),
                new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b"));
        assertCode("a", move.getLeftEntity());
        assertCode("b", move.getRightEntity());

        move = new SwapMove<>(Arrays.asList(primaryDescriptor, secondaryDescriptor),
                new TestdataMultiVarEntity("c"), new TestdataMultiVarEntity("d"));
        assertCode("c", move.getLeftEntity());
        assertCode("d", move.getRightEntity());
    }

    @Test
    void toStringTest() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var a = new TestdataEntity("a", null);
        var b = new TestdataEntity("b", v1);
        var c = new TestdataEntity("c", v2);
        var entityDescriptor = TestdataEntity.buildEntityDescriptor();
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();

        assertThat(new SwapMove<>(variableDescriptorList, a, a)).hasToString("a {null} <-> a {null}");
        assertThat(new SwapMove<>(variableDescriptorList, a, b)).hasToString("a {null} <-> b {v1}");
        assertThat(new SwapMove<>(variableDescriptorList, a, c)).hasToString("a {null} <-> c {v2}");
        assertThat(new SwapMove<>(variableDescriptorList, b, c)).hasToString("b {v1} <-> c {v2}");
        assertThat(new SwapMove<>(variableDescriptorList, c, b)).hasToString("c {v2} <-> b {v1}");
    }

    @Test
    void toStringTestMultiVar() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var v4 = new TestdataValue("v4");
        var w1 = new TestdataOtherValue("w1");
        var w2 = new TestdataOtherValue("w2");
        var a = new TestdataMultiVarEntity("a", null, null, null);
        var b = new TestdataMultiVarEntity("b", v1, v3, w1);
        var c = new TestdataMultiVarEntity("c", v2, v4, w2);
        var entityDescriptor = TestdataMultiVarEntity.buildEntityDescriptor();
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();

        assertThat(new SwapMove<>(variableDescriptorList, a, a)).hasToString("a {null, null, null} <-> a {null, null, null}");
        assertThat(new SwapMove<>(variableDescriptorList, a, b)).hasToString("a {null, null, null} <-> b {v1, v3, w1}");
        assertThat(new SwapMove<>(variableDescriptorList, a, c)).hasToString("a {null, null, null} <-> c {v2, v4, w2}");
        assertThat(new SwapMove<>(variableDescriptorList, b, c)).hasToString("b {v1, v3, w1} <-> c {v2, v4, w2}");
        assertThat(new SwapMove<>(variableDescriptorList, c, b)).hasToString("c {v2, v4, w2} <-> b {v1, v3, w1}");
    }

    @Test
    void testEnableNearbyMixedModel() {
        var moveSelectorConfig = new SwapMoveSelectorConfig();
        assertThat(moveSelectorConfig.canEnableNearbyInMixedModels()).isFalse();
    }
}
