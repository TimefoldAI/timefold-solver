package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
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
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SubChainSwapMoveTest {

    private final GenuineVariableDescriptor<TestdataChainedSolution> variableDescriptor =
            TestdataChainedEntity.buildVariableDescriptorForChainedObject();
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
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1, b2, b3 });

        try (var ephemeralMoveDirector = new MoveDirector<>(innerScoreDirector).ephemeral()) {
            var scoreDirector = Mockito.spy(ephemeralMoveDirector.getScoreDirector());
            var move = new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                    new SubChain(Arrays.asList(a3, a4, a5)),
                    new SubChain(Arrays.asList(b2, b3)));
            move.doMoveOnly(scoreDirector);

            SelectorTestUtils.assertChain(a0, a1, a2, b2, b3);
            SelectorTestUtils.assertChain(b0, b1, a3, a4, a5);

            verify(scoreDirector).changeVariableFacade(variableDescriptor, a3, b1);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, b2, a2);
        }
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
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1, b2, b3 });

        try (var ephemeralMoveDirector = new MoveDirector<>(innerScoreDirector).ephemeral()) {
            var scoreDirector = Mockito.spy(ephemeralMoveDirector.getScoreDirector());
            var move = new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                    new SubChain(Arrays.asList(a2, a3, a4)),
                    new SubChain(Arrays.asList(b1, b2)));
            move.doMoveOnly(scoreDirector);

            SelectorTestUtils.assertChain(a0, a1, b1, b2, a5);
            SelectorTestUtils.assertChain(b0, a2, a3, a4, b3);

            verify(scoreDirector).changeVariableFacade(variableDescriptor, a2, b0);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, b3, a4);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, b1, a1);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, a5, b2);
        }
    }

    @Test
    void sameChainWithNoneBetween() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);
        var a6 = new TestdataChainedEntity("a6", a5);
        var a7 = new TestdataChainedEntity("a7", a6);

        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, a6, a7 });

        try (var ephemeralMoveDirector = new MoveDirector<>(innerScoreDirector).ephemeral()) {
            var scoreDirector = Mockito.spy(ephemeralMoveDirector.getScoreDirector());
            var move = new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                    new SubChain(Arrays.asList(a2, a3, a4)),
                    new SubChain(Arrays.asList(a5, a6)));
            move.doMoveOnly(scoreDirector);

            SelectorTestUtils.assertChain(a0, a1, a5, a6, a2, a3, a4, a7);

            verify(scoreDirector).changeVariableFacade(variableDescriptor, a5, a1);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, a2, a6);
            verify(scoreDirector).changeVariableFacade(variableDescriptor, a7, a4);
        }
    }

    @Test
    void rebase() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var b0 = new TestdataChainedAnchor("b0");
        var c0 = new TestdataChainedAnchor("c0");
        var c1 = new TestdataChainedEntity("c1", c0);

        var destinationA0 = new TestdataChainedAnchor("a0");
        var destinationA1 = new TestdataChainedEntity("a1", destinationA0);
        var destinationA2 = new TestdataChainedEntity("a2", destinationA1);
        var destinationA3 = new TestdataChainedEntity("a3", destinationA2);
        var destinationB0 = new TestdataChainedAnchor("b0");
        var destinationC0 = new TestdataChainedAnchor("c0");
        var destinationC1 = new TestdataChainedEntity("c1", destinationC0);

        ScoreDirector<TestdataChainedSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { a0, destinationA0 },
                        { a1, destinationA1 },
                        { a2, destinationA2 },
                        { a3, destinationA3 },
                        { b0, destinationB0 },
                        { c0, destinationC0 },
                        { c1, destinationC1 },
                });
        var inverseVariableSupply = mock(SingletonInverseVariableSupply.class);

        assertSameProperties(Arrays.asList(destinationA1, destinationA2, destinationA3), Arrays.asList(destinationC1),
                new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                        new SubChain(Arrays.asList(a1, a2, a3)), new SubChain(Arrays.asList(c1)))
                        .rebase(destinationScoreDirector));
        assertSameProperties(Arrays.asList(destinationA1, destinationA2), Arrays.asList(destinationA3),
                new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                        new SubChain(Arrays.asList(a1, a2)), new SubChain(Arrays.asList(a3)))
                        .rebase(destinationScoreDirector));
    }

    public void assertSameProperties(List<Object> leftEntityList, List<Object> rightEntityList, SubChainSwapMove move) {
        assertThat(move.getLeftSubChain().getEntityList()).hasSameElementsAs(leftEntityList);
        assertThat(move.getRightSubChain().getEntityList()).hasSameElementsAs(rightEntityList);
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

        var variableDescriptor = TestdataChainedEntity
                .buildVariableDescriptorForChainedObject();
        var inverseVariableSupply = SelectorTestUtils.mockSingletonInverseVariableSupply(
                new TestdataChainedEntity[] { a1, a2, a3, a4, a5, b1, b2, b3 });

        assertThat(new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                new SubChain(Arrays.asList(a2, a3, a4)), new SubChain(Arrays.asList(b1, b2, b3))))
                .hasToString("[a2..a4] {a1} <-> [b1..b3] {b0}");
        assertThat(new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                new SubChain(Arrays.asList(a1, a2)), new SubChain(Arrays.asList(a4, a5))))
                .hasToString("[a1..a2] {a0} <-> [a4..a5] {a3}");
        assertThat(new SubChainSwapMove<>(variableDescriptor, inverseVariableSupply,
                new SubChain(Arrays.asList(a3)), new SubChain(Arrays.asList(b2))))
                .hasToString("[a3..a3] {a2} <-> [b2..b2] {b1}");
    }

}
