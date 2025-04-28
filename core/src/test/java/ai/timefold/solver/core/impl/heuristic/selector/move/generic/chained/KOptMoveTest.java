package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.anchor.AnchorVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class KOptMoveTest {

    private final GenuineVariableDescriptor<TestdataChainedSolution> variableDescriptor = TestdataChainedEntity
            .buildVariableDescriptorForChainedObject();
    private final SolutionDescriptor<TestdataChainedSolution> solutionDescriptor = variableDescriptor.getEntityDescriptor()
            .getSolutionDescriptor();
    private final InnerScoreDirector<TestdataChainedSolution, SimpleScore> innerScoreDirector =
            PlannerTestUtils.mockScoreDirector(solutionDescriptor);

    @Test
    void doMove3OptWith3Chains() {
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);

        var c0 = new TestdataChainedAnchor("c0");
        var c1 = new TestdataChainedEntity("c1", c0);
        var c2 = new TestdataChainedEntity("c2", c1);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1, b2, c1, c2));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3);
        SelectorTestUtils.assertChain(b0, b1, b2);
        SelectorTestUtils.assertChain(c0, c1, c2);

        var scoreDirector = moveDirector.getScoreDirector();
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, new Object[] { b0, c1 });
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, c2);
        SelectorTestUtils.assertChain(b0, a2, a3);
        SelectorTestUtils.assertChain(c0, c1, b1, b2);
    }

    @Test
    void doMove3OptWith3ChainsToTailValue() {
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);

        var c0 = new TestdataChainedAnchor("c0");
        var c1 = new TestdataChainedEntity("c1", c0);
        var c2 = new TestdataChainedEntity("c2", c1);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1, b2, c1, c2));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3);
        SelectorTestUtils.assertChain(b0, b1, b2);
        SelectorTestUtils.assertChain(c0, c1, c2);

        var scoreDirector = moveDirector.getScoreDirector();
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, new Object[] { b2, c2 });
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1);
        SelectorTestUtils.assertChain(b0, b1, b2, a2, a3);
        SelectorTestUtils.assertChain(c0, c1, c2);
    }

    @Test
    void doMove3OptWithOnly2Chains() {
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, a4, b1, b2, b3));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, b3);

        var scoreDirector = moveDirector.getScoreDirector();
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a4, new Object[] { a1, b2 });
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, a2, a3, b3);
    }

    @Test
    void doMove3OptWithOnly2ChainsSameMoveDifferentOrder() {
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, a4, b1, b2, b3));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, b3);

        var scoreDirector = moveDirector.getScoreDirector();
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, new Object[] { b2, a3 });
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, a2, a3, b3);
    }

    @Test
    void doMove3OptWithOnly2ChainsSameMoveYetDifferentOrder() {
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, a4, b1, b2, b3));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, b3);

        var scoreDirector = moveDirector.getScoreDirector();
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, b3, new Object[] { a3, a1 });
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();
        moveDirector.execute(move);

        SelectorTestUtils.assertChain(a0, a1, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, a2, a3, b3);
    }

    @Test
    void loopNotDoable() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);
        var a4 = new TestdataChainedEntity("a4", a3);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);
        var b3 = new TestdataChainedEntity("b3", b2);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, a4, b1, b2, b3));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        SelectorTestUtils.assertChain(a0, a1, a2, a3, a4);
        SelectorTestUtils.assertChain(b0, b1, b2, b3);

        // These moves would create a loop
        var move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a2, new Object[] { a3, b2 });
        assertThat(move.isMoveDoable(innerScoreDirector)).isFalse();
        move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, a4, new Object[] { b2, a1 });
        assertThat(move.isMoveDoable(innerScoreDirector)).isFalse();
        move = new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply, b3, new Object[] { a1, a3 });
        assertThat(move.isMoveDoable(innerScoreDirector)).isFalse();
    }

    @Test
    void toStringTest() {
        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);
        var b2 = new TestdataChainedEntity("b2", b1);

        var c0 = new TestdataChainedAnchor("c0");
        var c1 = new TestdataChainedEntity("c1", c0);
        var c2 = new TestdataChainedEntity("c2", c1);

        var solution = new TestdataChainedSolution("solution");
        solution.setChainedAnchorList(Arrays.asList(a0, b0, c0));
        solution.setChainedEntityList(Arrays.asList(a1, a2, a3, b1, b2, c1, c2));

        innerScoreDirector.setWorkingSolution(solution);
        var inverseVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        var anchorVariableSupply = innerScoreDirector.getSupplyManager()
                .demand(new AnchorVariableDemand<>(variableDescriptor));

        assertThat(new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply,
                a2, new Object[] { b0, c1 })).hasToString("a2 {a1} -kOpt-> b1 {b0} -kOpt-> c2 {c1}");
        assertThat(new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply,
                a2, new Object[] { b2, c2 })).hasToString("a2 {a1} -kOpt-> null {b2} -kOpt-> null {c2}");
    }

}
