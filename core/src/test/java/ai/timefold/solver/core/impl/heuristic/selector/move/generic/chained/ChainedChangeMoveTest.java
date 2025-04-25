package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class ChainedChangeMoveTest {

    private final GenuineVariableDescriptor<TestdataChainedSolution> variableDescriptor = TestdataChainedEntity
            .buildVariableDescriptorForChainedObject();
    private final InnerScoreDirector<TestdataChainedSolution, SimpleScore> innerScoreDirector =
            PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

    @Test
    void noTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, b1 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new ChainedChangeMove<>(variableDescriptor, a3, b1, inverseVariableSupply);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a2);
        SelectorTestUtils.assertChain(b0, b1, a3);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
    }

    @Test
    void oldAndNewTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, b1 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new ChainedChangeMove<>(variableDescriptor, a2, b0, inverseVariableSupply);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a3);
        SelectorTestUtils.assertChain(b0, a2, b1);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, b1);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, b1);
    }

    @Test
    void sameChainWithOneBetween() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new ChainedChangeMove<>(variableDescriptor, a2, a3, inverseVariableSupply);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a3, a2, a4);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
    }

    @Test
    void sameChainWithItself() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4 });

        var move = new ChainedChangeMove<>(variableDescriptor, a2, a2, inverseVariableSupply);
        assertThat(move.isMoveDoable(innerScoreDirector)).isFalse();
    }

    @Test
    void sameChainWithSamePlanningValue() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4 });

        var move = new ChainedChangeMove<>(variableDescriptor, a2, a1, inverseVariableSupply);
        assertThat(move.isMoveDoable(innerScoreDirector)).isFalse();
    }

    @Test
    void rebase() {
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
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { a0, destinationA0 },
                        { a1, destinationA1 },
                        { a2, destinationA2 },
                        { b0, destinationB0 },
                        { c1, destinationC1 },
                });
        var inverseVariableSupply = mock(SingletonInverseVariableSupply.class);

        assertSameProperties(destinationA1, null,
                new ChainedChangeMove<>(variableDescriptor, a1, null, inverseVariableSupply).rebase(destinationScoreDirector));
        assertSameProperties(destinationA2, destinationB0,
                new ChainedChangeMove<>(variableDescriptor, a2, b0, inverseVariableSupply).rebase(destinationScoreDirector));
        assertSameProperties(destinationC1, destinationA2,
                new ChainedChangeMove<>(variableDescriptor, c1, a2, inverseVariableSupply).rebase(destinationScoreDirector));
    }

    public void assertSameProperties(Object entity, Object toPlanningVariable, ChainedChangeMove<?> move) {
        assertSoftly(softly -> {
            softly.assertThat(move.getEntity()).isSameAs(entity);
            softly.assertThat(move.getToPlanningValue()).isSameAs(toPlanningVariable);
        });
    }

}
