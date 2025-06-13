package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.anchor.AnchorVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.anchor.AnchorVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class TailChainSwapMoveTest {

    private final GenuineVariableDescriptor<TestdataChainedSolution> variableDescriptor = TestdataChainedEntity
            .buildVariableDescriptorForChainedObject();
    private final SolutionDescriptor<TestdataChainedSolution> solutionDescriptor = variableDescriptor.getEntityDescriptor()
            .getSolutionDescriptor();
    private final InnerScoreDirector<TestdataChainedSolution, SimpleScore> innerScoreDirector =
            PlannerTestUtils.mockScoreDirector(solutionDescriptor);

    @Test
    void isMoveDoable() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, b0)
                .isMoveDoable(innerScoreDirector)).isTrue();
        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, b1, a1)
                .isMoveDoable(innerScoreDirector)).isTrue();
        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a1, a2)
                .isMoveDoable(innerScoreDirector)).isTrue();
        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a3, a0)
                .isMoveDoable(innerScoreDirector)).isTrue();
        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a1, a1)
                .isMoveDoable(innerScoreDirector)).isFalse();
        assertThat(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, a0)
                .isMoveDoable(innerScoreDirector)).isFalse();
    }

    @Test
    void doMove() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3);
        SelectorTestUtils.assertChain(b0, b1);

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.execute(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, b0));
        SelectorTestUtils.assertChain(a0, a1, b1);
        SelectorTestUtils.assertChain(b0, a2, a3);
    }

    @Test
    void doMoveToTailValue() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3);
        SelectorTestUtils.assertChain(b0, b1);

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.execute(new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, b1));
        SelectorTestUtils.assertChain(a0, a1);
        SelectorTestUtils.assertChain(b0, b1, a2, a3);
    }

    @Test
    void doMoveInSameChain() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);
        var a5 = new TestdataChainedEntity("a5", a4);
        var a6 = new TestdataChainedEntity("a6", a5);
        var a7 = new TestdataChainedEntity("a7", a6);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, a4, a5, a6, a7));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));
        SelectorTestUtils.assertChain(a0, a1, a2, a3, a4, a5, a6, a7);

        var moveDirector = new MoveDirector<>(innerScoreDirector);
        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a4, a1),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a1, a4, a3, a2, a5, a6, a7);
                    return null;
                });

        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a3, a1),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a1, a3, a2, a4, a5, a6, a7);
                    return null;
                });

        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a7, a1),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a1, a7, a6, a5, a4, a3, a2);
                    return null;
                });

        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a1, a4),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a7, a6, a5, a2, a3, a4, a1);
                    return null;
                });

        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a3, a4),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a7, a6, a5, a4, a3, a2, a1);
                    return null;
                });

        moveDirector.executeTemporary(
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, a6),
                (__, ___) -> {
                    SelectorTestUtils.assertChain(a0, a7, a3, a4, a5, a6, a2, a1);
                    return null;
                });
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
        var anchorVariableSupply = mock(AnchorVariableSupply.class);

        assertSameProperties(destinationA1, destinationC1,
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a1, c1)
                        .rebase(destinationScoreDirector));
        assertSameProperties(destinationA3, destinationA0,
                new TailChainSwapMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a3, a0)
                        .rebase(destinationScoreDirector));
    }

    public void assertSameProperties(Object leftEntity, Object rightValue, TailChainSwapMove<TestdataChainedSolution> move) {
        assertThat(move.getLeftEntity()).isSameAs(leftEntity);
        assertThat(move.getRightValue()).isSameAs(rightValue);
    }

    @Test
    void toStringTest() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        assertThat(new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, a1, b0)).hasToString("a1 {a0} <-tailChainSwap-> b1 {b0}");
        assertThat(new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, a1, b1)).hasToString("a1 {a0} <-tailChainSwap-> null {b1}");
        assertThat(new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, b1, a0)).hasToString("b1 {b0} <-tailChainSwap-> a1 {a0}");
        assertThat(new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, a1, a3)).hasToString("a1 {a0} <-tailChainSwap-> null {a3}");
        assertThat(new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, a2, a0)).hasToString("a2 {a1} <-tailChainSwap-> a1 {a0}");
    }

    @Test
    void extractPlanningEntitiesWithRightEntityNull() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", null);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", null);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, b1));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        var move = new TailChainSwapMove<>(variableDescriptor,
                inverseVariableSupply, anchorVariableSupply, a1, b0);
        assertThat(move.getPlanningEntities()).doesNotContainNull();

        move.doMoveOnGenuineVariables(innerScoreDirector);
        assertThat(move.getPlanningEntities()).doesNotContainNull();
    }

    @Test
    void testEnableNearbyMixedModel() {
        var moveSelectorConfig = new TailChainSwapMoveSelectorConfig();
        assertThat(moveSelectorConfig.canEnableNearbyInMixedModels()).isFalse();
    }
}
