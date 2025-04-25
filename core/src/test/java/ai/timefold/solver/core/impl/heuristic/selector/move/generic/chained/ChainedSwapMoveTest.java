package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static java.util.Arrays.asList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class ChainedSwapMoveTest {

    private final GenuineVariableDescriptor<TestdataChainedSolution> chainedVariableDescriptor = TestdataChainedEntity
            .buildVariableDescriptorForChainedObject();
    private final GenuineVariableDescriptor<TestdataChainedSolution> unchainedVariableDescriptor = TestdataChainedEntity
            .buildVariableDescriptorForUnchainedValue();
    private final InnerScoreDirector<TestdataChainedSolution, SimpleScore> innerScoreDirector =
            PlannerTestUtils.mockScoreDirector(chainedVariableDescriptor.getEntityDescriptor().getSolutionDescriptor(), false);

    @Test
    void noTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        a1.setUnchainedValue(new TestdataValue(a1.getCode()));
        var a2 = new TestdataChainedEntity("a2", a1);
        a2.setUnchainedValue(new TestdataValue(a2.getCode()));
        var a3 = new TestdataChainedEntity("a3", a2);
        a3.setUnchainedValue(new TestdataValue(a3.getCode()));

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        b1.setUnchainedValue(new TestdataValue(b1.getCode()));

        var originalA1UnchainedObject = a1.getUnchainedValue();
        var originalA2UnchainedObject = a2.getUnchainedValue();
        var originalA3UnchainedObject = a3.getUnchainedValue();
        var originalB1UnchainedObject = b1.getUnchainedValue();

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, b1 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.execute(new ChainedSwapMove<>(asList(chainedVariableDescriptor, unchainedVariableDescriptor),
                asList(inverseVariableSupply, null), a3, b1));

        assertSoftly(softly -> {
            softly.assertThat(a1.getUnchainedValue()).isEqualTo(originalA1UnchainedObject);
            softly.assertThat(a2.getUnchainedValue()).isEqualTo(originalA2UnchainedObject);
            softly.assertThat(a3.getUnchainedValue()).isEqualTo(originalB1UnchainedObject);
            softly.assertThat(b1.getUnchainedValue()).isEqualTo(originalA3UnchainedObject);
        });
        SelectorTestUtils.assertChain(a0, a1, a2, b1);
        SelectorTestUtils.assertChain(b0, a3);

        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, b1);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, b1);
    }

    @Test
    void oldAndNewTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        a1.setUnchainedValue(new TestdataValue(a1.getCode()));
        var a2 = new TestdataChainedEntity("a2", a1);
        a2.setUnchainedValue(new TestdataValue(a2.getCode()));
        var a3 = new TestdataChainedEntity("a3", a2);
        a3.setUnchainedValue(new TestdataValue(a3.getCode()));

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        b1.setUnchainedValue(new TestdataValue(b1.getCode()));
        var b2 = new TestdataChainedEntity("b2", b1);
        b2.setUnchainedValue(new TestdataValue(b2.getCode()));

        var originalA1UnchainedObject = a1.getUnchainedValue();
        var originalA2UnchainedObject = a2.getUnchainedValue();
        var originalA3UnchainedObject = a3.getUnchainedValue();
        var originalB1UnchainedObject = b1.getUnchainedValue();
        var originalB2UnchainedObject = b2.getUnchainedValue();

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, b1, b2 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.execute(new ChainedSwapMove<>(asList(chainedVariableDescriptor, unchainedVariableDescriptor),
                asList(inverseVariableSupply, null), a2, b1));

        assertSoftly(softly -> {
            softly.assertThat(a1.getUnchainedValue()).isEqualTo(originalA1UnchainedObject);
            softly.assertThat(a2.getUnchainedValue()).isEqualTo(originalB1UnchainedObject);
            softly.assertThat(a3.getUnchainedValue()).isEqualTo(originalA3UnchainedObject);
            softly.assertThat(b1.getUnchainedValue()).isEqualTo(originalA2UnchainedObject);
            softly.assertThat(b2.getUnchainedValue()).isEqualTo(originalB2UnchainedObject);
        });

        SelectorTestUtils.assertChain(a0, a1, b1, a3);
        SelectorTestUtils.assertChain(b0, a2, b2);

        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a2);
        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, b1);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, b1);
        verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, b2);
        verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, b2);
    }

    @Test
    void sameChain() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        a1.setUnchainedValue(new TestdataValue(a1.getCode()));
        var a2 = new TestdataChainedEntity("a2", a1);
        a2.setUnchainedValue(new TestdataValue(a2.getCode()));
        var a3 = new TestdataChainedEntity("a3", a2);
        a3.setUnchainedValue(new TestdataValue(a3.getCode()));
        var a4 = new TestdataChainedEntity("a4", a3);
        a4.setUnchainedValue(new TestdataValue(a4.getCode()));

        var originalA1UnchainedObject = a1.getUnchainedValue();
        var originalA2UnchainedObject = a2.getUnchainedValue();
        var originalA3UnchainedObject = a3.getUnchainedValue();
        var originalA4UnchainedObject = a4.getUnchainedValue();

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.executeTemporary(
                new ChainedSwapMove<>(asList(chainedVariableDescriptor, unchainedVariableDescriptor),
                        asList(inverseVariableSupply, null), a2, a3),
                (__, ___) -> {
                    assertSoftly(softly -> {
                        softly.assertThat(a1.getUnchainedValue()).isEqualTo(originalA1UnchainedObject);
                        softly.assertThat(a2.getUnchainedValue()).isEqualTo(originalA3UnchainedObject);
                        softly.assertThat(a3.getUnchainedValue()).isEqualTo(originalA2UnchainedObject);
                        softly.assertThat(a4.getUnchainedValue()).isEqualTo(originalA4UnchainedObject);
                    });
                    SelectorTestUtils.assertChain(a0, a1, a3, a2, a4);

                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a2);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a2);
                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a3);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a3);
                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a4);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a4);
                    return null;
                });

        clearInvocations(innerScoreDirector);
        moveDirector.executeTemporary(
                new ChainedSwapMove<>(asList(chainedVariableDescriptor, unchainedVariableDescriptor),
                        asList(inverseVariableSupply, null), a3, a2),
                (__, ___) -> {
                    assertSoftly(softly -> {
                        softly.assertThat(a1.getUnchainedValue()).isEqualTo(originalA1UnchainedObject);
                        softly.assertThat(a2.getUnchainedValue()).isEqualTo(originalA3UnchainedObject);
                        softly.assertThat(a3.getUnchainedValue()).isEqualTo(originalA2UnchainedObject);
                        softly.assertThat(a4.getUnchainedValue()).isEqualTo(originalA4UnchainedObject);
                    });
                    SelectorTestUtils.assertChain(a0, a1, a3, a2, a4);

                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a2);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a2);
                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a3);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a3);
                    verify(innerScoreDirector).beforeVariableChanged(chainedVariableDescriptor, a4);
                    verify(innerScoreDirector).afterVariableChanged(chainedVariableDescriptor, a4);
                    return null;
                });

    }

    @Test
    void rebase() {
        var entityDescriptor = TestdataChainedEntity.buildEntityDescriptor();
        var variableDescriptorList = entityDescriptor
                .getGenuineVariableDescriptorList();

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var b0 = new TestdataChainedAnchor("b0");
        var c1 = new TestdataChainedEntity("c1", null);

        var destinationA0 = new TestdataChainedAnchor("a0");
        var destinationA1 = new TestdataChainedEntity("a1", destinationA0);
        var destinationA2 = new TestdataChainedEntity("a2", destinationA1);
        var destinationB0 = new TestdataChainedAnchor("b0");
        var destinationC1 = new TestdataChainedEntity("c1", null);

        ScoreDirector<TestdataChainedSolution> destinationScoreDirector = mockRebasingScoreDirector(
                entityDescriptor.getSolutionDescriptor(), new Object[][] {
                        { a0, destinationA0 },
                        { a1, destinationA1 },
                        { a2, destinationA2 },
                        { b0, destinationB0 },
                        { c1, destinationC1 },
                });
        var inverseVariableSupplyList = Collections.singletonList(
                mock(SingletonInverseVariableSupply.class));

        assertSameProperties(destinationA1, destinationA2,
                new ChainedSwapMove<>(variableDescriptorList, inverseVariableSupplyList, a1, a2)
                        .rebase(destinationScoreDirector));
        assertSameProperties(destinationA1, destinationC1,
                new ChainedSwapMove<>(variableDescriptorList, inverseVariableSupplyList, a1, c1)
                        .rebase(destinationScoreDirector));
        assertSameProperties(destinationA2, destinationC1,
                new ChainedSwapMove<>(variableDescriptorList, inverseVariableSupplyList, a2, c1)
                        .rebase(destinationScoreDirector));
    }

    public void assertSameProperties(Object leftEntity, Object rightEntity, ChainedSwapMove<?> move) {
        assertSoftly(softly -> {
            softly.assertThat(move.getLeftEntity()).isSameAs(leftEntity);
            softly.assertThat(move.getRightEntity()).isSameAs(rightEntity);
        });
    }

}
