package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedSwapMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SelectorBasedCompositeMoveTest {

    @Test
    void doMove() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        var a = mock(SelectorBasedDummyMove.class);
        when(a.isMoveDoable(any())).thenReturn(true);
        var b = mock(SelectorBasedDummyMove.class);
        when(b.isMoveDoable(any())).thenReturn(true);
        var c = mock(SelectorBasedDummyMove.class);
        when(c.isMoveDoable(any())).thenReturn(true);
        var move = new SelectorBasedCompositeMove<>(a, b, c);
        move.execute(scoreDirector);
        verify(a, times(1)).execute(any(VariableDescriptorAwareScoreDirector.class));
        verify(b, times(1)).execute(any(VariableDescriptorAwareScoreDirector.class));
        verify(c, times(1)).execute(any(VariableDescriptorAwareScoreDirector.class));
    }

    @Test
    void rebase() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

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

        InnerScoreDirector<TestdataSolution, ?> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        var a = new SelectorBasedChangeMove<>(variableDescriptor, e1, v2);
        var b = new SelectorBasedChangeMove<>(variableDescriptor, e2, v1);
        var rebaseMove = new SelectorBasedCompositeMove<>(a, b).rebase(destinationScoreDirector.getMoveDirector());
        var rebasedChildMoves = rebaseMove.getMoves();
        assertThat(rebasedChildMoves).hasSize(2);
        var rebasedA = (SelectorBasedChangeMove<TestdataSolution>) rebasedChildMoves[0];
        assertThat(rebasedA.getEntity()).isSameAs(destinationE1);
        assertThat(rebasedA.getToPlanningValue()).isSameAs(destinationV2);
        var rebasedB = (SelectorBasedChangeMove<TestdataSolution>) rebasedChildMoves[1];
        assertThat(rebasedB.getEntity()).isSameAs(destinationE2);
        assertThat(rebasedB.getToPlanningValue()).isSameAs(destinationV1);
    }

    @Test
    void buildEmptyMove() {
        assertThat(SelectorBasedCompositeMove.buildMove(new ArrayList<>()))
                .isInstanceOf(SelectorBasedNoChangeMove.class);
        assertThat(SelectorBasedCompositeMove.buildMove())
                .isInstanceOf(SelectorBasedNoChangeMove.class);
    }

    @Test
    void buildOneElemMove() {
        var tmpMove = new SelectorBasedDummyMove();
        var move = SelectorBasedCompositeMove.buildMove(Collections.singletonList(tmpMove));
        assertThat(move)
                .isInstanceOf(SelectorBasedDummyMove.class);

        move = SelectorBasedCompositeMove.buildMove(tmpMove);
        assertThat(move)
                .isInstanceOf(SelectorBasedDummyMove.class);
    }

    @Test
    <Solution_> void buildTwoElemMove() {
        Move<Solution_> first = (Move<Solution_>) new SelectorBasedDummyMove();
        Move<Solution_> second = SelectorBasedNoChangeMove.getInstance();
        var move = SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move)
                .isInstanceOf(SelectorBasedCompositeMove.class);
        assertThat(((SelectorBasedCompositeMove<TestdataSolution>) move).getMoves()[0])
                .isInstanceOf(SelectorBasedDummyMove.class);
        assertThat(((SelectorBasedCompositeMove<TestdataSolution>) move).getMoves()[1])
                .isInstanceOf(SelectorBasedNoChangeMove.class);

        move = SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move)
                .isInstanceOf(SelectorBasedCompositeMove.class);
        assertThat(((SelectorBasedCompositeMove<TestdataSolution>) move).getMoves()[0])
                .isInstanceOf(SelectorBasedDummyMove.class);
        assertThat(((SelectorBasedCompositeMove<TestdataSolution>) move).getMoves()[1])
                .isInstanceOf(SelectorBasedNoChangeMove.class);
    }

    @Test
    void isMoveDoable() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());

        var first = new SelectorBasedDummyMove();
        var second = new SelectorBasedDummyMove();
        var move = (SelectorBasedCompositeMove<TestdataSolution>) SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new SelectorBasedDummyMove();
        second = new SelectorBasedNotDoableDummyMove();
        move = (SelectorBasedCompositeMove<TestdataSolution>) SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new SelectorBasedNotDoableDummyMove();
        second = new SelectorBasedDummyMove();
        move = (SelectorBasedCompositeMove<TestdataSolution>) SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new SelectorBasedNotDoableDummyMove();
        second = new SelectorBasedNotDoableDummyMove();
        move = (SelectorBasedCompositeMove<TestdataSolution>) SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isFalse();
    }

    @Test
    <Solution_> void equals() {
        var first = (Move<Solution_>) new SelectorBasedDummyMove();
        var second = (Move<Solution_>) SelectorBasedNoChangeMove.getInstance();
        var move = SelectorBasedCompositeMove.buildMove(Arrays.asList(first, second));
        var other = SelectorBasedCompositeMove.buildMove(first, second);
        assertThat(move).isEqualTo(other);

        move = SelectorBasedCompositeMove.buildMove(first, second);
        other = SelectorBasedCompositeMove.buildMove(second, first);
        assertThat(move)
                .isNotEqualTo(other)
                .isNotEqualTo(new SelectorBasedDummyMove());
    }

    @Test
    void interconnectedChildMoves() {
        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);
        solution.setEntityList(Arrays.asList(e1, e2));

        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var first = new SelectorBasedChangeMove<>(variableDescriptor, e1, v3);
        var second = new SelectorBasedSwapMove<>(Collections.singletonList(variableDescriptor), e1, e2);
        var move = SelectorBasedCompositeMove.buildMove(first, second);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);

        try (var scoreDirector = mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor())) {
            move.execute(scoreDirector.getMoveDirector());
        }

        assertThat(e1.getValue()).isSameAs(v2);
        assertThat(e2.getValue()).isSameAs(v3);
    }

}
