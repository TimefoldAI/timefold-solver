package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SubChainReversingChangeMoveTest {

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
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a3, a4, a5)), variableDescriptor,
                inverseVariableSupply, b1);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a2);
        SelectorTestUtils.assertChain(b0, b1, a5, a4, a3);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
    }

    @Test
    void oldAndNewTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a2, a3, a4)), variableDescriptor,
                inverseVariableSupply, b0);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a5);
        SelectorTestUtils.assertChain(b0, a4, a3, a2, b1);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, b1);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, b1);
    }

    @Test
    void sameChainInPlaceNoTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a3, a4, a5)), variableDescriptor,
                inverseVariableSupply, a2);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a2, a5, a4, a3);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
    }

    @Test
    void sameChainInPlaceOldAndNewTrailing() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a2, a3, a4)), variableDescriptor,
                inverseVariableSupply, a1);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a4, a3, a2, a5);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a2);
    }

    @Test
    void sameChainWithOneBetween() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5 });

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var move = new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a1, a2, a3)), variableDescriptor,
                inverseVariableSupply, a4);
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a4, a3, a2, a1, a5);

        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a4);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a3);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a2);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a1);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a1);
        verify(innerScoreDirector).beforeVariableChanged(variableDescriptor, a5);
        verify(innerScoreDirector).afterVariableChanged(variableDescriptor, a5);
    }

    @Test
    void rebase() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var b0 = new TestdataChainedAnchor("b0");
        var c1 = new TestdataChainedEntity("c1", null);

        var destinationA0 = new TestdataChainedAnchor("a0");
        var destinationA1 = new TestdataChainedEntity("a1", destinationA0);
        var destinationA2 = new TestdataChainedEntity("a2", destinationA1);
        var destinationA3 = new TestdataChainedEntity("a3", destinationA2);
        var destinationB0 = new TestdataChainedAnchor("b0");
        var destinationC1 = new TestdataChainedEntity("c1", null);

        ScoreDirector<TestdataChainedSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { a0, destinationA0 },
                        { a1, destinationA1 },
                        { a2, destinationA2 },
                        { a3, destinationA3 },
                        { b0, destinationB0 },
                        { c1, destinationC1 },
                });
        var inverseVariableSupply = mock(SingletonInverseVariableSupply.class);

        assertSameProperties(Arrays.asList(destinationA1, destinationA2), null,
                new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a1, a2)), variableDescriptor,
                        inverseVariableSupply, null).rebase(destinationScoreDirector));
        assertSameProperties(Arrays.asList(destinationA1, destinationA2, destinationA3), destinationB0,
                new SubChainReversingChangeMove<>(new SubChain(Arrays.asList(a1, a2, a3)), variableDescriptor,
                        inverseVariableSupply, b0).rebase(destinationScoreDirector));
    }

    public void assertSameProperties(List<Object> entityList, Object toPlanningVariable, SubChainReversingChangeMove move) {
        assertThat(move.getSubChain().getEntityList()).hasSameElementsAs(entityList);
        assertThat(move.getToPlanningValue()).isSameAs(toPlanningVariable);
    }

    @Test
    void toStringTest() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1, b2, b3 });

        assertThat(new SubChainReversingChangeMove<>(
                new SubChain(Arrays.asList(a2, a3, a4)), variableDescriptor, inverseVariableSupply, b0))
                .hasToString("[a2..a4] {a1 -reversing-> b0}");
        assertThat(new SubChainReversingChangeMove<>(
                new SubChain(Arrays.asList(a1, a2, a3, a4, a5)), variableDescriptor, inverseVariableSupply, b3))
                .hasToString("[a1..a5] {a0 -reversing-> b3}");
        assertThat(new SubChainReversingChangeMove<>(
                new SubChain(Arrays.asList(a1, a2, a3)), variableDescriptor, inverseVariableSupply, a5))
                .hasToString("[a1..a3] {a0 -reversing-> a5}");
        assertThat(new SubChainReversingChangeMove<>(
                new SubChain(Arrays.asList(a3)), variableDescriptor, inverseVariableSupply, b2))
                .hasToString("[a3..a3] {a2 -reversing-> b2}");
    }

}
